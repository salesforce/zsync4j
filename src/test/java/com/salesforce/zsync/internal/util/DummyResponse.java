/**
 *
 * Copyright (c) 2020, Bitshift (bitshifted.co), Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.zsync.internal.util;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Dummy response class used in tests.
 *
 * @author Vladimir Djurovic
 */
public class DummyResponse implements HttpResponse {

	private final int statusCode;
	private final HttpRequest request;
	private HttpHeaders headers;

	public DummyResponse(int statusCode, HttpRequest request){
		this.statusCode = statusCode;
		this.request = request;
	}

	@Override
	public int statusCode() {
		return statusCode;
	}

	@Override
	public HttpRequest request() {
		return request;
	}

	@Override
	public Optional<HttpResponse> previousResponse() {
		return Optional.empty();
	}

	@Override
	public HttpHeaders headers() {
		return headers;
	}

	@Override
	public Object body() {
		return null;
	}

	@Override
	public Optional<SSLSession> sslSession() {
		return Optional.empty();
	}

	@Override
	public URI uri() {
		return null;
	}

	@Override
	public HttpClient.Version version() {
		return null;
	}

	public void setHeader(Map<String, List<String>> headersMap) {
		headers = HttpHeaders.of(headersMap, new BiPredicate<String, String>() {
			@Override
			public boolean test(String s, String s2) {
				return true;
			}
		});
	}
}
