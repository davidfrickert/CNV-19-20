package pt.ist.meic.cnv.SudokuSolver.balancer;

import com.amazonaws.services.stepfunctions.model.MissingRequiredParameterException;
import org.springframework.util.MultiValueMap;
import pt.ist.meic.cnv.SudokuSolver.dynamodb.DynamoDBHelper;
import pt.ist.meic.cnv.SudokuSolver.dynamodb.Metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Request implements IRequest {

    private static final String TABLE_NAME = "Server-metrics";

    private String solver;
    private Integer unassigned;

    private Integer nColumns;
    private Integer nLines;

    public Request(MultiValueMap<String, String> requestParams) {
        boolean allRequiredParams = Stream.of("s", "un", "n1", "n2", "i").allMatch(requestParams::containsKey);
        Map<String, String> requestParameters = requestParams.toSingleValueMap();
        if (! allRequiredParams) throw new MissingRequiredParameterException("Missing required params. You need to supply\n" +
                "'s': solver strategy, 'un': unassigned entry number, 'n1': number of columns, 'n2': number of lines, " +
                "'i': image name");
        solver = requestParameters.get("s");
        unassigned = Integer.parseInt(requestParameters.get("un"));
        nColumns = Integer.parseInt(requestParameters.get("n1"));
        nLines = Integer.parseInt(requestParameters.get("n2"));

    }

    @Override
    public long estimateRequestLoad() {
        DynamoDBHelper dbh = DynamoDBHelper.getInstance();
        List<Metrics> metrics = DynamoDBHelper.convertGenericResults(dbh.scan(TABLE_NAME, getRequestParams()));
        // case of existing previous requests similar to this one
        if (metrics.size() > 0) {
            Metrics avg = Metrics.average(metrics);
            long load = avg.calculateLoad();
            System.out.println("Estimated by dynamodb: " + load);
            return load;
        } else {
            // no similar requests to this one - base heuristic
            long load = (nLines * nColumns * 10) + unassigned;
            System.out.println("Estimated by heuristic: " + load);
            return load;
        }
    }

    public Map<String, Object> getRequestParams() {
        return new HashMap<>() {{
            put("Columns", nColumns);
            put("Lines", nLines);
            put("Solver-Type", solver);
            put("Unassigned-Entries", unassigned);
        }};
    }
}
