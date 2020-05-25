package pt.ist.meic.cnv.SudokuSolver.balancer;

import com.amazonaws.services.ec2.model.Instance;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class InstanceInfo implements IInstanceInfo{
    private final Instance InstanceData;
    private final List<Request> currentRequests;

    public InstanceInfo(Instance instanceData) {
        InstanceData = instanceData;
        currentRequests = new ArrayList<>();
    }

    @Override
    public long calculateInstanceLoad() {
        synchronized (currentRequests) {
            return getCurrentRequests().stream().map(IRequest::estimateRequestLoad).reduce(0L, Long::sum);
        }
    }

    public void addRequest(Request r) {
        currentRequests.add(r);
    }

    public void removeRequest(Request r) {
        currentRequests.remove(r);
    }


    @Override
    public String toString() {
        return "InstanceInfo{" +
                "InstanceID=" + InstanceData.getInstanceId() +
                ", calculateInstanceLoad=" + calculateInstanceLoad() +
                '}';
    }
}
