/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.appengine.search;

// @formatter:off
// [START search_document_import]

import com.google.appengine.api.search.*;
import org.json.simple.JSONObject;
// [END search_document_import]

// CHECKSTYLE:OFF
// @formatter:on
// CHECKSTYLE:ON

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@SuppressWarnings("serial")
public class SearchServlet extends HttpServlet {

  private static final String SEARCH_INDEX = "HCHotels";

  private Index getIndex() {
    IndexSpec indexSpec = IndexSpec.newBuilder().setName(SEARCH_INDEX).build();
    Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);
    return index;
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
          throws IOException {
    PrintWriter out = resp.getWriter();
    String name = req.getParameter("name");
    String jsonResult;
    // [START search_document]
    final int maxRetry = 3;
    int attempts = 0;
    int delay = 2;
    while (true) {
      try {
        String queryString = "HotelName = " + name;
        QueryOptions option = QueryOptions.newBuilder().setLimit(20).build();
        Query query = Query.newBuilder().setOptions(option).build(queryString);
        Results<ScoredDocument> results = getIndex().search(query);
        JSONObject obj = new JSONObject();
        obj.put("Result", results);
        jsonResult = obj.toJSONString();

      } catch (SearchException e) {
        if (StatusCode.TRANSIENT_ERROR.equals(e.getOperationResult().getCode())
                && ++attempts < maxRetry) {
          // retry
          try {
            Thread.sleep(delay * 1000);
          } catch (InterruptedException e1) {
            // ignore
          }
          delay *= 2; // easy exponential backoff
          continue;
        } else {
          throw e;
        }
      }
      break;
    }
    // [END search_document]
    // We don't test the search result below, but we're fine if it runs without errors.
    out.println(jsonResult);

    // [END simple_search_3]
  }
}
