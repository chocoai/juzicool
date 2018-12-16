package com.juzicool.gather.store;

import java.io.*;
import java.sql.*;
import java.util.HashMap;

/**
 * KV 键值
 */
public class JuziDB {

    private final String jdbcUrl;
    private Connection mConnection = null;
    private final File mFile;

    public JuziDB(File file){
        mFile = file;
        jdbcUrl = "jdbc:sqlite:" +file.getAbsolutePath();
    }

    public void prepare(){
        // db parameters
        try {
            mConnection = DriverManager.getConnection(jdbcUrl);
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n" + KEY + "  text PRIMARY KEY,\n"
                    + VALUE + " BLOB);";
            Statement stmt = mConnection.createStatement();
            stmt.execute(sql);
            stmt.closeOnCompletion();
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }

    }

    public void close(){
        try {
            if (mConnection != null) {
                mConnection.close();
            }
            mConnection = null;
        } catch (SQLException ex) {

        }
    }


    public void putKv(String key, Serializable value){
        String sql = "INSERT or replace INTO "+TABLE_NAME+"("+KEY+", "+VALUE+") VALUES(?,?)";
        PreparedStatement pstmt = null;
        try {
            Connection conn = mConnection;
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, key);
            pstmt.setBytes(2, objectToByte(value));
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw  new RuntimeException(e);
        }finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            }catch (Exception ex){

            }

        }
    }

    public <T extends Serializable> T getKv(String key,T defaultValue){
        String sql = "SELECT "+VALUE+" FROM " + TABLE_NAME +" where " + KEY +" ='" +key +"'";
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Connection conn = mConnection;
            stmt= conn.createStatement();
            rs = stmt.executeQuery(sql);
            byte[] byteData = null;
            // loop through the result set
            while (rs.next()) {
                byteData = rs.getBytes(1);
            }
            if(byteData!=null){
                Serializable obj = byteToObject(byteData);
                return (T) obj;
            }

        } catch (Exception e) {
            throw  new RuntimeException(e);
        }finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            }catch (Exception ex){

            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            }catch (Exception ex){

            }

        }

        return defaultValue;
    }

    public static Serializable byteToObject(byte[] bytes) {
        Serializable obj = null;
        try {
            // bytearray to object
            ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
            ObjectInputStream oi = new ObjectInputStream(bi);

            obj = (Serializable) oi.readObject();
            bi.close();
            oi.close();
        } catch (Exception e) {
            System.out.println("translation" + e.getMessage());
            e.printStackTrace();
        }
        return obj;
    }

    public static byte[] objectToByte(Serializable obj) {
        byte[] bytes = null;
        try {
            // object to bytearray
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);

            bytes = bo.toByteArray();

            bo.close();
            oo.close();
        } catch (Exception e) {
            System.out.println("translation" + e.getMessage());
            e.printStackTrace();
        }
        return bytes;
    }



    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String TABLE_NAME = "kv_db";



    public static void main(String[] args) {
       /* File sqlFile = new File("connect.db");

        JuziDB db = JuziDB.request(sqlFile);
        db.putKv("name","sser");
        db.putKv("name2",234);

        String name = db.getKv("name",null);
        Integer integr = db.getKv("name2",null);

        System.out.println(String.format("name:%s,name2:%d",name,integr));
        System.out.println("null: " + db.getKv("www","shout null"));

        JuziDB.release(db);*/
    }

}
