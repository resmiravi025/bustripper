package io.telenor.bustripper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 *
 */
public class BusStopsCallBack implements InvocationCallback<Response> {

    private ObjectMapper mapper = new ObjectMapper();

    private TripsCallback listener;

    public BusStopsCallBack(TripsCallback callback) {
        this.listener = callback;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void completed(Response response) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            BusStop[] stops = mapper.readValue(response.readEntity(String.class), BusStop[].class);

            List<BusStop> stopsList = Arrays.stream(stops).filter(x->x.getPlaceType().equalsIgnoreCase("stop")).collect(Collectors.toList());

            if(stopsList.isEmpty())
                throw new IOException("Failed to get bus stops");

            BusStop[] newStops = new BusStop[stopsList.size()];
            newStops = stopsList.toArray(newStops);


            System.out.println(String.format("Got %d busstops nearby", newStops.length));
            CountDownLatch latch = new CountDownLatch(newStops.length-1);
            for(int i = 0; i< newStops.length;i++) {
                BusStop stop = newStops[i];
                boolean isLast = stop == newStops[newStops.length -1];
                new Thread(new FindBusLinesForStop(stop.getId(), listener, isLast, latch)).start();
            }
        } catch (IOException e) {
            listener.failedGettingTrips(e);
        }

    }

    public void failed(Throwable throwable) {
        listener.failedGettingTrips((IOException) throwable);
    }
}
