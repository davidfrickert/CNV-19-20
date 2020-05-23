package pt.ist.meic.cnv.webapp.Balancer;

import com.amazonaws.services.ec2.model.Instance;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pt.ist.meic.cnv.webapp.SudokuSolver.exception.NoInstanceAvailable;
import pt.ist.meic.cnv.webapp.SudokuSolver.instance.InstanceInfo;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class LoadBalancer {
    private ConcurrentHashMap<String, InstanceInfo> currentInstances = new ConcurrentHashMap<>();

    public LoadBalancer() {
        System.out.println("Lbal init");
    }

    public InstanceInfo getBestInstance() throws NoInstanceAvailable {
        Map<Long, InstanceInfo> allInstanceLoads = calculateLoadOfAllInstances();
        System.out.println("All loads: " + allInstanceLoads);
        Optional<Map.Entry<Long, InstanceInfo>> optionalMin = allInstanceLoads.entrySet().stream()
                .min(Comparator.comparingLong(Map.Entry::getKey));
        if (optionalMin.isPresent())
            return optionalMin.get().getValue();
        throw new NoInstanceAvailable("No instance available");
    }

    public Map<Long, InstanceInfo> calculateLoadOfAllInstances() {
        return currentInstances.values().stream().map(instanceInfo ->
                new AbstractMap.SimpleEntry<>(instanceInfo.calculateInstanceLoad(), instanceInfo)
        ).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    public void addInstance(Instance instanceName) {
        if (! currentInstances.containsKey(instanceName.getInstanceId()))
            currentInstances.put(instanceName.getInstanceId(), new InstanceInfo(instanceName));
    }

    public InstanceInfo removeInstance(String instanceID) {
        return currentInstances.remove(instanceID);
    }


    @Override
    public String toString() {
        return "LoadBalancer{" +
                "currentInstances=" + currentInstances +
                '}';
    }
}
