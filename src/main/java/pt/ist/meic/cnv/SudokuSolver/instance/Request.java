package pt.ist.meic.cnv.SudokuSolver.instance;

import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.stepfunctions.model.MissingRequiredParameterException;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.stream.Stream;

public class Request implements IRequest {

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
    //TODO
    @Override
    public long estimateRequestLoad() {
        return 0;
    }
}
