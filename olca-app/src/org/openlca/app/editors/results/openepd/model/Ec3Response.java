package org.openlca.app.editors.results.openepd.model;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class Ec3Response {

  private final int status;
  private final Map<String, List<String>> headers;
  private final JsonElement json;

  private Ec3Response(HttpResponse<?> resp, JsonElement json) {
    status = resp.statusCode();
    headers = new HashMap<>();
    resp.headers().map().forEach((key, values) -> {
      var k = key.trim().toLowerCase();
      headers.put(k, new ArrayList<>(values));
    });
    this.json = json;
  }

  static Ec3Response of(HttpResponse<InputStream> resp) {
    JsonElement json;
    try (var stream = resp.body();
         var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      json = new Gson().fromJson(reader, JsonElement.class);
    } catch (Exception e) {
      json = null;
    }
    return new Ec3Response(resp, json);
  }

  public int status() {
    return status;
  }

  public String header(String key) {
    var list = headers(key);
    return list.isEmpty()
      ? ""
      : list.get(0);
  }

  public List<String> headers(String key) {
    var list = headers.get(key);
    return list == null
      ? Collections.emptyList()
      : list;
  }

  public boolean hasJson() {
    return json != null;
  }

  public JsonElement json() {
    return json;
  }

  /**
   * Returns the {@code x-total-count} header from the response. Returns 0 if no
   * such header is available.
   */
  public int totalCount() {
    return getIntHeader("x-total-count");
  }

  /**
   * Returns the {@code x-page-size} header from the response. Returns 0 if no
   * such header is available.
   */
  public int pageSize() {
    return getIntHeader("x-page-size");
  }

  /**
   * Returns the {@code x-total-pages} header from the response. Returns 0 if no
   * such header is available.
   */
  public int pageCount() {
    return getIntHeader("x-total-pages");
  }


  private int getIntHeader(String key) {
    var s = header(key);
    if (s == null || s.isBlank())
      return 0;
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      return 0;
    }
  }
}
