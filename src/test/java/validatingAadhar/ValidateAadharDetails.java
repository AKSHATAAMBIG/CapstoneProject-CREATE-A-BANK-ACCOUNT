package validatingAadhar;

import common.method.Reusable;
import db.connection.DatabaseConnection;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;

import static common.method.Reusable.*;
import static common.method.Reusable.getAadharDetailsFromDB;
import static io.restassured.RestAssured.given;


public class ValidateAadharDetails {

    Reusable rs;
    DatabaseConnection dc;

    @BeforeClass
    public void setUp(){
        rs = new Reusable();
        dc = new DatabaseConnection();
    }

    @Test(priority = 0)
    public void validateDatabaseConnection(){
        dc.createNewDatabase();
        dc.createNewTable();
        dc.insertDataIntoTable();
    }

    // Read Aadhar_no from properties file and match with Aadhar DB , if it is correct data
    //then do below API call to create a new BANK ACCOUNT

    @Parameters({"url"})
    @Test(priority = 1)
    public void validateAadharNumberProperty_DB(String url) {
        String aadhar_no_property = null;
        List<String>aadharDetailsFromDB = null;
        boolean matchFound = false;
        aadhar_no_property = readPropertyFile();

        aadharDetailsFromDB = getAadharDetailsFromDB(aadhar_no_property);

        matchFound = aadharDetailsFromDB.contains(aadhar_no_property);

        if(matchFound)
        {
            System.out.println("Aadhar number matches\n"+"Aadhaar Number from property:"+aadhar_no_property+" \nAadhar number from database:"+aadharDetailsFromDB.get(2));

            //if Aadhar number matches with database and property file crete a new bank account
            createBankAccount(url, aadharDetailsFromDB.get(0),aadharDetailsFromDB.get(1),aadharDetailsFromDB.get(2),aadharDetailsFromDB.get(3),aadharDetailsFromDB.get(4));
        } else
        {
            System.out.println("Aadhar number does not match.");
        }
    }

    public void createBankAccount(String url,String firstName,String lastName,String aadharNumber,String address,String phoneNumber) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(rs.createJsonBody(firstName, lastName, aadharNumber, address, phoneNumber))
                .when()
                .post(url);

        int statuscode = response.getStatusCode();
        String responseBody = response.getBody().asString();
        System.out.println("Status code:"+statuscode);
        System.out.println("Response is:\n"+responseBody);
        validateResponseAndDBDetails(responseBody,firstName, lastName, aadharNumber, address, phoneNumber);

    }

    // Read the response and match the response Fname,Lname,Aadhar_No,Address,Phone data with DB.
    public static void validateResponseAndDBDetails(String responseBody,String firstName,String lastName,String aadharNumber,String address,String phoneNumber){
        JSONObject jsonObject = new JSONObject(responseBody);

        String responseFirstName = jsonObject.getString("Fname");
        String responseLastName = jsonObject.getString("Lname");
        String responseAadharNumber = jsonObject.getString("Aadhar_No");
        String responseAddress = jsonObject.getString("Address");
        String responsePhone = jsonObject.getString("Phone");

        Assert.assertEquals(responseFirstName, firstName);
        Assert.assertEquals(responseLastName, lastName);
        Assert.assertEquals(responseAadharNumber, aadharNumber);
        Assert.assertEquals(responseAddress, address);
        Assert.assertEquals(responsePhone, phoneNumber);

        System.out.println("***** Details from database*****");
        System.out.println("Fname:"+firstName +" Lname:" +lastName+" Aadhar_No:"+aadharNumber + " Address:"+address + " Phone:"+ phoneNumber);

        //Validate Account ID should be created in Response.
        String responseAccountId = jsonObject.getString("AccountId");
        Assert.assertNotNull(responseAccountId);

        //Validate ID should be numeric.
        String responseId = jsonObject.getString("id");
        Assert.assertTrue(responseId.matches("\\d+"));

        //Validate createdAt in response and its date should be current date.
        String responseCreatedAt = jsonObject.getString("createdAt");
        Assert.assertTrue(isCurrentDate(responseCreatedAt));

        System.out.println("Account ID: " + responseAccountId);
        System.out.println("createdAt: " + responseCreatedAt);

    }

    @AfterClass
    public void tearDown(){
        System.out.println("tear down");
    }
}

