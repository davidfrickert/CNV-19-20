package pt.ist.meic.cnv.webapp.SudokuSolver.dynamodb;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import lombok.Getter;
import pt.ist.meic.cnv.webapp.SudokuSolver.exception.MissingRequiredParameterException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class Metrics {
    private String id;

    private String solverType;
    private Integer lines;
    private Integer columns;
    private Long unassignedEntries;

    private Long methodCount;
    private Long newArrayCount;
    private Long newObjectCount;
    private Long newMultiRefArrayCount;
    private Long newRefArrayCount;

    private static final List<String> REQUEST_PARAMS = Arrays.asList("Columns", "Lines", "Solver-Type", "Unassigned-Entries");
    private static final List<String> INSTRUMENTATION_PARAMS = Arrays.asList("Method-counter", "New-Array-counter",
            "New-Multi-Reference-counter", "New-Object-counter", "New-Reference-Array-counter");
    private static final List<String> PARAMS = Stream.concat(REQUEST_PARAMS.stream(), INSTRUMENTATION_PARAMS.stream())
            .collect(Collectors.toList());

    private static String colsParam = "Columns";
    private static String linesParam = "Lines";
    private static String solverTypeParam = "Solver-Type";
    private static String unassignedEntriesParam = "Unassigned-Entries";

    private static String methodsParam = "Method-counter";
    private static String arraysParam = "New-Array-counter";
    private static String multiRefParam = "New-Multi-Reference-counter";
    private static String objectsParam = "New-Object-counter";
    private static String refArraysParam = "New-Reference-Array-counter";

    private static final String primaryKey = "id";

    static {
        PARAMS.add(primaryKey);
    }

    public static <T> boolean requiredParameters(Map<String, T> namesAndValues) {
        return PARAMS.stream().allMatch(namesAndValues::containsKey);
    }

    public Metrics(Map<String, AttributeValue> dynamoDBScanResult) {
        if (! requiredParameters(dynamoDBScanResult))
            throw new MissingRequiredParameterException("Missing params.");

        id = dynamoDBScanResult.get(primaryKey).getS();

        solverType = dynamoDBScanResult.get(solverTypeParam).getS();
        lines = Integer.parseInt(dynamoDBScanResult.get(linesParam).getN());
        columns = Integer.parseInt(dynamoDBScanResult.get(colsParam).getN());
        unassignedEntries = Long.parseLong(dynamoDBScanResult.get(unassignedEntriesParam).getN());

        methodCount = Long.parseLong(dynamoDBScanResult.get(methodsParam).getN());
        newArrayCount = Long.parseLong(dynamoDBScanResult.get(arraysParam).getN());
        newObjectCount = Long.parseLong(dynamoDBScanResult.get(objectsParam).getN());
        newMultiRefArrayCount = Long.parseLong(dynamoDBScanResult.get(multiRefParam).getN());
        newRefArrayCount = Long.parseLong(dynamoDBScanResult.get(refArraysParam).getN());
    }

    public Metrics(String id, String solverType, Integer lines, Integer columns, Long unassignedEntries, Long methodCount, Long newArrayCount, Long newObjectCount, Long newMultiRefArrayCount, Long newRefArrayCount) {
        this.id = id;
        this.solverType = solverType;
        this.lines = lines;
        this.columns = columns;
        this.unassignedEntries = unassignedEntries;
        this.methodCount = methodCount;
        this.newArrayCount = newArrayCount;
        this.newObjectCount = newObjectCount;
        this.newMultiRefArrayCount = newMultiRefArrayCount;
        this.newRefArrayCount = newRefArrayCount;
    }

    public static Metrics average(List<Metrics> metricsToMakeAvg) {
        Integer lines, columns;
        String solverType = null;
        Long unassignedEntries, methodCount, newArrayCount, newObjectCount, newMultiRefArrayCount, newRefArrayCount;

        lines = columns = 0;
        unassignedEntries = methodCount = newArrayCount = newObjectCount = newMultiRefArrayCount
                = newRefArrayCount = 0L;

        for (Metrics m1 : metricsToMakeAvg) {
            solverType = m1.getSolverType();
            lines += m1.getLines();
            columns += m1.getColumns();
            unassignedEntries += m1.getUnassignedEntries();

            methodCount += m1.getMethodCount();
            newArrayCount += m1.getNewArrayCount();
            newObjectCount += m1.getNewObjectCount();
            newMultiRefArrayCount += m1.getNewMultiRefArrayCount();
            newRefArrayCount += m1.getNewRefArrayCount();
        }

        return new Metrics(null, solverType, lines, columns, unassignedEntries, methodCount, newArrayCount, newObjectCount
            ,newMultiRefArrayCount, newRefArrayCount);
    }

    public Long calculateLoad() {
        return methodCount + newObjectCount + newMultiRefArrayCount + newRefArrayCount + (newArrayCount / 2);
    }


    @Override
    public String toString() {
        return "Metrics{" +
                "id='" + id + '\'' +
                ", solverType='" + solverType + '\'' +
                ", lines=" + lines +
                ", columns=" + columns +
                ", unassignedEntries=" + unassignedEntries +
                ", methodCount=" + methodCount +
                ", newArrayCount=" + newArrayCount +
                ", newObjectCount=" + newObjectCount +
                ", newMultiRefArrayCount=" + newMultiRefArrayCount +
                ", newRefArrayCount=" + newRefArrayCount +
                '}';
    }
}
