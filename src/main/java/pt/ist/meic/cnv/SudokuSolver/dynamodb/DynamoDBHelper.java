package pt.ist.meic.cnv.SudokuSolver.dynamodb;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import pt.ist.meic.cnv.SudokuSolver.balancer.exception.MissingRequiredParameterException;


import java.util.*;
import java.util.stream.Collectors;

public class DynamoDBHelper {

    private AmazonDynamoDB dynamoDBClient;

    private void init() {
        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();

        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        dynamoDBClient = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(Regions.EU_WEST_1)
                .build();
    }

    private DynamoDBHelper() {
        init();
    }

    private static DynamoDBHelper INSTANCE;

    public static DynamoDBHelper getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DynamoDBHelper();
        return INSTANCE;
    }

    public Condition makeEqualsCondition(Object value) {
        Condition cond =  new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString());
        if (value instanceof String)
            return cond.withAttributeValueList(new AttributeValue().withS((String) value));
        else if (value instanceof Integer || value instanceof Long)
            return cond.withAttributeValueList(new AttributeValue().withN(value.toString()));
        else throw new IllegalArgumentException("Method only for String / Integer / Long");
    }

    public List<Map<String, AttributeValue>> scan(String tableName, Map<String, Object> params) {

        // Required params
        String cols = "Columns";
        String lines = "Lines";
        String solverType = "Solver-Type";
        String unassignedEntries = "Unassigned-Entries";

        List<String> reqParams = Arrays.asList(cols, lines, solverType, unassignedEntries);
        boolean allReqParams = reqParams.stream().allMatch(params::containsKey);
        if (!allReqParams)
            throw new MissingRequiredParameterException("Missing required parameters. Required parameters: " + reqParams + ", supplied: "
            + params.keySet());

        // Values extracted
        String methods = "Method-counter";
        String arrays = "New-Array-counter";
        String multiRef = "New-Multi-Reference-counter";
        String objects = "New-Object-counter";
        String refArrays = "New-Reference-Array-counter";

        HashMap<String, Condition> scanFilter = new HashMap<>();

        Condition colsEq = makeEqualsCondition(params.get(cols));
        Condition linesEq = makeEqualsCondition(params.get(lines));
        Condition solverEq = makeEqualsCondition(params.get(solverType));
        Condition unEq = makeEqualsCondition(params.get(unassignedEntries));

        scanFilter.put(cols, colsEq);
        scanFilter.put(lines, linesEq);
        scanFilter.put(solverType, solverEq);
        scanFilter.put(unassignedEntries, unEq);

        ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
        ScanResult scanResult = dynamoDBClient.scan(scanRequest);

        return scanResult.getItems();
    }

    public static List<Metrics>  convertGenericResults(List<Map<String, AttributeValue>> resultsDynamoDB) {
        return resultsDynamoDB.stream().map(Metrics::new).collect(Collectors.toList());
    }
}
