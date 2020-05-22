package pt.ist.meic.cnv.webapp;

import com.amazonaws.services.directconnect.model.Loa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ist.meic.cnv.webapp.AutoScaler.AutoScaler;
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

    @Autowired
    private AutoScaler autoScaler;

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

    public LoadBalancer() {
        System.out.println("Initalized LB");
        System.out.println("LB:AS: " + autoScaler);
    }


    @Override
    public String toString() {
        return "LoadBalancer{" +
                "currentInstances=" + currentInstances +
                '}';
    }
}
