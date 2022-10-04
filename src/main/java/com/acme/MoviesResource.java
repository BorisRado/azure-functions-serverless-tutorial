package com.acme;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Produces(MediaType.APPLICATION_JSON)
@Path("movies")
public class MoviesResource {

    private static Logger log = Logger.getLogger(MoviesResource.class.getName());
    private static List<String> movies = new ArrayList<>();

    @GET
    public Response getMovies() {
        log.info("Received GET request!");
        return Response.status(Response.Status.OK).entity(movies).build();
    }

    @POST
    public Response postMovie(String movieName) {
        if (movieName.isEmpty()) {
            log.warning("Received POST request with empty body.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            log.info("Received POST request for movie " + movieName + "!");
            movies.add(movieName);
            return Response.status(Response.Status.CREATED).build();
        }
    }

}
