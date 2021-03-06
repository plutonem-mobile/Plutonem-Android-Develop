package com.plutonem.android.networking;

import com.android.volley.RequestQueue;
import com.plutonem.rest.RestClient;

public class RestClientFactoryDefault implements RestClientFactoryAbstract {
    public RestClient make(RequestQueue queue) {
        return new RestClient(queue);
    }

    public RestClient make(RequestQueue queue, RestClient.REST_CLIENT_VERSIONS version) {
        return new RestClient(queue, version);
    }
}