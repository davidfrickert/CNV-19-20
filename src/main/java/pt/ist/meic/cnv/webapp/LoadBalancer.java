package pt.ist.meic.cnv.webapp;

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
@Order(value = 1)
public class LoadBalancer {
    private ConcurrentHashMap<String, InstanceInfo> currentInstances = new ConcurrentHashMap<>();

    public LoadBalancer() {
        System.out.println("Lbal init");
    }

    public InstanceInfo getBestInstance() throws NoInstanceAvailable {
        Optional<Map.Entry<Long, InstanceInfo>> optionalMin = calculateLoadOfAllInstances().entrySet().stream()
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
        currentInstances.put(instanceName.getInstanceId(), new InstanceInfo(instanceName));
        System.out.println(currentInstances);
    }


    @Override
    public String toString() {
        return "LoadBalancer{" +
                "currentInstances=" + currentInstances +
                '}';
    }
}
