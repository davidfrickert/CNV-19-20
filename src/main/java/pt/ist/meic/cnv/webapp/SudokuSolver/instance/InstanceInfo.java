package pt.ist.meic.cnv.webapp.SudokuSolver.instance;

import com.amazonaws.services.ec2.model.Instance;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class InstanceInfo implements IInstanceInfo{
    private final String InstanceData;
    private List<Request> currentRequests;

    public InstanceInfo(String instanceData) {
        InstanceData = instanceData;
        currentRequests = new ArrayList<>();
    }

    @Override
    public long calculateInstanceLoad() {
        return getCurrentRequests().stream().map(IRequest::estimateRequestLoad).reduce(0L, Long::sum);
    }

    public void addRequest(Request r) {
        currentRequests.add(r);
    }

    public void removeRequest(Request r) {
        currentRequests.remove(r);
    }
}
