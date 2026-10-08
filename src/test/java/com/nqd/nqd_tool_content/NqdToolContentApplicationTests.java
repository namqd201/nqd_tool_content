package com.nqd.nqd_tool_content;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;

class NqdToolContentApplicationTests {

    @Test
    void testEmbeddedUrl() {
        String fullUrl = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?user=postgres.avrdcnnhsyqyvbvtbaoi&password=B6hhzUiN4GYuUds5&sslmode=require";
        try (Connection conn = DriverManager.getConnection(fullUrl)) {
            System.out.println(">>> EMBEDDED URL CONNECTED SUCCESSFULLY! Catalog: " + conn.getCatalog());
        } catch (Exception e) {
            System.err.println(">>> FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
