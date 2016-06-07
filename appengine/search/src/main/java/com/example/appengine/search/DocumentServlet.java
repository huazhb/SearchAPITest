/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.appengine.search;

// [START document_import]
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.GeoPoint;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;
// [END document_import]

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.example.appengine.business.Hotel;

/**
 * A servlet for creating Search API Document.
 */
@SuppressWarnings("serial")
public class DocumentServlet extends HttpServlet {
  public Document createDocument(Hotel hotel) {
    // [START create_document]
    String myDocId = String.valueOf(hotel.hotelID);
    Document doc = Document.newBuilder()
            .setId(myDocId).addField(Field.newBuilder().setName("HotelName").setText(hotel.hotelName))
            .addField(Field.newBuilder().setName("Location").setGeoPoint(new GeoPoint(hotel.latitude.doubleValue(), hotel.longitude.doubleValue())))
            .build();
    // [END create_document]
    return doc;
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    String connString;
    if (System.getProperty("com.google.appengine.runtime.version").startsWith("Google App Engine/")) {
      connString = System.getProperty("mysql");
      try {
        // Load the class that provides the new "jdbc:google:mysql://"
        // prefix.
        Class.forName("com.mysql.jdbc.GoogleDriver");
      } catch (ClassNotFoundException e) {
        throw new ServletException("Error loading Google JDBC Driver", e);
      }
    } else {
      // Set the url with the local MySQL database connection url when
      // running locally
      connString = System.getProperty("mysqlLocal");
    }

    ArrayList hotels = new ArrayList();
    try (Connection conn = DriverManager.getConnection(connString)) {
      String selectSql = "select HotelID, HotelName, Latitude, Longitude from Hotel where IsImported = 0 order by HotelID Limit 50000";
      ResultSet rs = conn.prepareStatement(selectSql).executeQuery();
      while (rs.next()) {
        Hotel hotel = new Hotel();
        hotel.hotelID = rs.getInt("HotelID");
        hotel.hotelName = rs.getString("HotelName");
        hotel.latitude = rs.getBigDecimal("Latitude");
        hotel.longitude = rs.getBigDecimal("Longitude");
        hotels.add(hotel);
      }

      int batchSize = 200;
      String indexName = "HCHotels";
      ArrayList docs = new ArrayList();
      Index index = null;
      for(int i = 0; i < hotels.size(); i++){
        if(docs.size() == batchSize){
          index = Utils.indexDocuments(index, indexName, docs);
          docs.clear();
        }
        Document doc = createDocument((Hotel) hotels.get(i));
        docs.add(doc);
      }
      if(docs.size() > 0){
        Utils.indexDocuments(index, indexName, docs);
        docs.clear();
      }

      String updateSql = "update Hotel set IsImported = 1 where HotelID between ? and ?";
      PreparedStatement statement = conn.prepareStatement(updateSql);
      statement.setInt(1, ((Hotel)hotels.get(0)).hotelID);
      statement.setInt(2, ((Hotel)hotels.get(hotels.size() -1)).hotelID);
      statement.execute();

      req.setAttribute("hotels", hotels.size());
      try{
        req.getRequestDispatcher("/welcome.jsp").forward(req, resp);
      }
      catch(ServletException e){
        throw new ServletException("error forward to welcome.jsp", e);
      }
    } catch (SQLException e) {
      throw new ServletException("SQL error", e);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
