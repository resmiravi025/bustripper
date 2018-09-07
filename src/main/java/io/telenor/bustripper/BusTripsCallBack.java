package io.telenor.bustripper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

/**
 * Callback from Jersey when bustrips are there.
 */
public class BusTripsCallBack implements InvocationCallback<Response> {
    ObjectMapper mapper = new ObjectMapper();
    String url;
    private TripsCallback listener;
    private boolean last;
    private CountDownLatch latch;

    public BusTripsCallBack(String url, TripsCallback callback, boolean last, CountDownLatch latch) {
        this.url = url;
        this.listener = callback;
        this.last = last;
        this.latch = latch;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void completed(Response response) {
        ObjectMapper mapper = new ObjectMapper();
        String content = response.readEntity(String.class);

        try {
            BusTrip[] trips = mapper.readValue(content, BusTrip[].class);
            HashSet set = new HashSet(Arrays.asList(trips));
            if (last && Thread.activeCount() > 1) {
                this.latch.await();
            } else {
                this.latch.countDown();
            }
            listener.gotTrips(set, last);

        } catch (IOException e) {
            if (last) {
                listener.failedGettingTrips(e);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void failed(Throwable throwable) {
        listener.failedGettingTrips((IOException) throwable);
    }
}
