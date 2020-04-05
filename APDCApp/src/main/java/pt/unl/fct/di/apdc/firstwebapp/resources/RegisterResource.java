package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;

import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;
import pt.unl.fct.di.apdc.firstwebapp.util.userRoles;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {

	/**
	 * Logger Object
	 */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	// Instantiates a client
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	public RegisterResource() { }

	@POST
	@Path("/v2")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRegistrationV2(RegisterData data) {
		if( ! data.validRegistration() ) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}

		LOG.fine("Attempt to register user: " + data.username);

		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = datastore.get(userKey);	
			if (user != null ) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User already exists.").build();
			}else {
				user = Entity.newBuilder(userKey)
						.set("user_name", data.username)
						.set("user_pwd", DigestUtils.sha512Hex(data.password))
						.set("user_email", data.email)
						.set("user_profile", data.profile)
						.set("user_landlinePhone", data.landlinePhone)
						.set("user_mobilePhone", data.mobilePhone)	
						.set("user_street", data.street)
						.set("user_accState", data.accState)
						.set("user_role", userRoles.USER.toString())
						.set("user_creation_time", Timestamp.now())
						.build();
				txn.add(user);

				LOG.info("User registered " + data.username);
				txn.commit();
				return Response.ok().build();
			}
		} finally {
			if(txn.isActive()) {
				txn.rollback();
			}
		}
	}
}

