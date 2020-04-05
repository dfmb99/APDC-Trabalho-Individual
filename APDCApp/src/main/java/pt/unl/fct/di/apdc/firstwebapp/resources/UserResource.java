package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.firstwebapp.util.AccProfile;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;
import pt.unl.fct.di.apdc.firstwebapp.util.userRoles;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class UserResource {

	/**
	 * Logger Object
	 */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	// Instantiates a client
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();

	public UserResource() { }

	@POST
	@Path("/data")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response getUserDataV2(AuthToken token) {
		Key tokenKey = datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", token.username))
				.setKind("Tokens").newKey(token.username);

		Entity utoken = datastore.get(tokenKey);
		if( utoken == null || !utoken.getString("tokenID").equals(token.tokenID)) {
			return Response.status(Status.FORBIDDEN).entity("AuthToken invalid... Try to login again!").build();
		}
		try {
			Query<Entity> query = Query.newEntityQueryBuilder()
					.setKind("User")
					.setFilter(CompositeFilter.and(PropertyFilter.eq("user_name", token.username)))
					.build();

			QueryResults<Entity> data = datastore.run(query);
			Map<String, String> udata = new HashMap<String, String>();
			Entity task = data.next();
			udata.put("username",task.getString("user_name"));
			udata.put("email", task.getString("user_email"));
			udata.put("profile", task.getString("user_profile"));
			udata.put("accState", task.getString("user_accState"));
			udata.put("role", task.getString("user_role"));
			udata.put("landlinePhone", task.getString("user_landlinePhone"));
			udata.put("mobilePhone", task.getString("user_mobilePhone"));
			udata.put("street", task.getString("user_street"));

			return Response.ok(g.toJson(udata)).build();
		}catch (Exception e){
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Path("/logout")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogoutV2(AuthToken token) {
		try {
			Key tokenKey = datastore.newKeyFactory()
					.addAncestors(PathElement.of("User", token.username))
					.setKind("Tokens").newKey(token.username);
			Entity utoken = datastore.get(tokenKey);
			if( utoken == null || !utoken.getString("tokenID").equals(token.tokenID)) {
				return Response.status(Status.FORBIDDEN).entity("AuthToken invalid... Try to login again!").build();
			}
			datastore.delete(tokenKey);
			LOG.info("User '" + token.username + "' logged out ");
			return Response.ok().build();
		}catch (Exception e){
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Path("/remove/{username}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRemoveUserV2(AuthToken token, @PathParam("username") String username) {
		try {
			Key tokenKey = datastore.newKeyFactory()
					.addAncestors(PathElement.of("User", token.username))
					.setKind("Tokens").newKey(token.username);
			Entity utoken = datastore.get(tokenKey);
			if( utoken == null || !utoken.getString("tokenID").equals(token.tokenID)) {
				return Response.status(Status.FORBIDDEN).entity("AuthToken invalid... Try to login again!").build();
			}
			//User that requested
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(token.username);
			Entity user = datastore.get(userKey);	
			//User to remove
			Key ruserKey = datastore.newKeyFactory().setKind("User").newKey(username);
			Entity ruser = datastore.get(ruserKey);	
			if (ruser == null ) {
				return Response.status(Status.BAD_REQUEST).entity("User does not exist.").build();
			}else if(user.getString("user_role").equalsIgnoreCase(userRoles.USER.toString()) && !token.username.equals(username)) {
				return Response.status(Status.FORBIDDEN).entity("Not enough permission to remove other users.").build();
			}else {
				datastore.delete(ruserKey);
				LOG.info("User '" + username + "' deleted.");
				return Response.ok().build();
			}
		}catch (Exception e){
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Path("/edit/{tokenID}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doEditUserV2(RegisterData data, @PathParam("tokenID") String tokenID) {
		if(data.username == null || data.profile == null || data.mobilePhone == null || data.landlinePhone == null || data.street == null || data.accState == null) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}

		Transaction txn = datastore.newTransaction();
		try {
			Key tokenKey = datastore.newKeyFactory()
					.addAncestors(PathElement.of("User", data.username))
					.setKind("Tokens").newKey(data.username);
			Entity utoken = datastore.get(tokenKey);
			if( utoken == null || !utoken.getString("tokenID").equals(tokenID)) {
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("AuthToken invalid... Try to login again!").build();
			}
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = datastore.get(userKey);	
			if (user == null ) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User does not exist.").build();
			} else {
				user = Entity.newBuilder(userKey)
						.set("user_name", user.getString("user_name"))
						.set("user_pwd", user.getString("user_pwd"))
						.set("user_email", user.getString("user_email"))
						.set("user_profile", data.profile)
						.set("user_landlinePhone", data.landlinePhone)
						.set("user_mobilePhone", data.mobilePhone)	
						.set("user_street", data.street)
						.set("user_accState", data.accState)
						.set("user_role", user.getString("user_role"))
						.set("user_creation_time", Timestamp.now())
						.build();
				txn.put(user);

				LOG.info("User '" + data.username + "' edited.");
				txn.commit();
				return Response.ok().build();
			}
		} finally {
			if(txn.isActive()) {
				txn.rollback();
			}
		}
	}

	@POST
	@Path("/list")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response doListPublicUsersrV2(AuthToken token) {
		Key tokenKey = datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", token.username))
				.setKind("Tokens").newKey(token.username);
		Entity utoken = datastore.get(tokenKey);
		if( utoken == null || !utoken.getString("tokenID").equals(token.tokenID)) {
			return Response.status(Status.FORBIDDEN).entity("AuthToken invalid... Try to login again!").build();
		}
		try {
			Query<Entity> query = Query.newEntityQueryBuilder()
					.setKind("User")
					.setFilter(CompositeFilter.and(PropertyFilter.eq("user_profile", "Public")))
					.build();

			QueryResults<Entity> data = datastore.run(query);
			List<String> udata = new ArrayList<String>();
			while(data.hasNext()) {
				Entity task = data.next();
				udata.add(task.getString("user_name"));
			}
			return Response.ok(g.toJson(udata)).build();
		}catch (Exception e){
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

	}
}


