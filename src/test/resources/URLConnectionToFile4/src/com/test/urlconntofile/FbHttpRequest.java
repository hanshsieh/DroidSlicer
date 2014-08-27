package com.test.urlconntofile;

import org.apache.http.client.methods.HttpUriRequest;

public class FbHttpRequest
{
	private final HttpUriRequest mRequest;
	public FbHttpRequest(HttpUriRequest request)
	{
		mRequest = request;
	}
	public HttpUriRequest getRequest()
	{
		return mRequest;
	}
}
