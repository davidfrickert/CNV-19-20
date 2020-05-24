package pt.ist.meic.cnv.webapp.Scaler;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import pt.ist.meic.cnv.webapp.Balancer.LoadBalancer;
import pt.ist.meic.cnv.webapp.SudokuSolver.instance.InstanceInfo;

@Service
public class AutoScaler extends Thread {

    private static AmazonEC2 ec2;
    private static AmazonCloudWatch cloudWatch;
    private HashMap<String, Double> instances = new HashMap<>();
    private String instanceToDelete = "none";
    private Double maximumValue = 70D;
    private Double minimumValue = 30D;
    private final String amiID = "ami-0200f098c4d50a07f";
    
    @Autowired
    private LoadBalancer lbal;

    public String getInstanceToDelete() {
        return instanceToDelete;
    }

    private static void init() {

        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (~/.aws/credentials).
         */
        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
        ec2 = AmazonEC2ClientBuilder.standard().withRegion(Regions.EU_WEST_1).withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
        cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(Regions.EU_WEST_1).withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
    }

    public AutoScaler(String initialID) throws Exception {
        init();
        instances.put(initialID, 0D);
    }

    public AutoScaler() {
        init();

        System.out.println("Autoscaler initialized.");

        start();
    }

    /**
     * Gets all the instances
     * @return the instances
     */
    private Set<Instance> getInstances(){
        Set<Instance> instancesTMP = new HashSet<Instance>();

        DescribeInstancesResult describeInstancesResult = ec2.describeInstances();
        List<Reservation> reservations = describeInstancesResult.getReservations();
        System.out.println("total reservations = " + reservations.size());
        for (Reservation reservation : reservations) {
            for (Instance i : reservation.getInstances()) {
                if(i.getImageId().equals(amiID))
                    instancesTMP.add(i);
            }
        }

        System.out.println("total instances = " + instances.size());
        return instancesTMP;
    }

    /**
     * launches a new instance
     */
    private void launchInstance(){
        System.out.println("launched instance");
        System.out.println(lbal);
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId(amiID)
                               .withInstanceType("t2.micro")
                               .withMinCount(1)
                               .withMaxCount(1)
                               .withKeyName("ssh-eu")
                               .withSecurityGroups("CNV");

        RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);

        Instance newInstance = runInstancesResult.getReservation().getInstances().get(0);

        instances.put(newInstance.getInstanceId(), 0D);
        lbal.addInstance(newInstance);

    }

    /**
     * Terminates instances
     * @param instanceId
     */
    private void terminateInstance(String instanceId){
        if (instances.size() == 1 || instanceToDelete.equals("none")) { return; }
        System.out.println("Terminating " + instanceId);
        InstanceInfo IInfo = lbal.removeInstance(instanceId);

        if (IInfo != null) {
            Thread waitForRequestsToFinish = new Thread(new WaitForReqsToFinish(IInfo));
            waitForRequestsToFinish.start();
        }
    }

    public void terminateInstance(){
        if (instances.size() == 1 || instanceToDelete.equals("none")) { return; }
        
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instanceToDelete);
        ec2.terminateInstances(termInstanceReq);
        instances.remove(instanceToDelete);

        instanceToDelete = "none";
    }

    private void setInstanceToDelete(String instanceID){
        if (instances.size() == 1) { return; }
        instanceToDelete = instanceID;
    }

    /**
     * The loop that updates the measures and checks if we should launch or terminate instances
     * @throws InterruptedException
     */
    public void loop() throws InterruptedException {
        while (true) {
            updateInstances();
            checkIfActionNeeded();

            Thread.sleep(60000);
        }
    }

    @Override
    public void run() {
        try {
            loop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if we need to terminate or launch a instance
     */
    public void checkIfActionNeeded(){
        Double avg = 0D;
        
        for (Double e : instances.values()) {
            avg += e;
        }

        avg /= instances.size();
        System.out.println("CPU AVG: " + avg);
        if (avg > maximumValue || instances.isEmpty()) {
            launchInstance();
        }

        if (avg < minimumValue) {
            String bestInstance = lbal.getBestInstance().getInstanceData().getInstanceId();
            if(instanceToDelete.equals("none")) {
                setInstanceToDelete(bestInstance);
                terminateInstance(bestInstance);
            }
        }
    }

    /**
     * Updates the Updates the instances measures
     */
    public void updateInstances() {

        Set<Instance> instancesTMP = getInstances();

        long offsetInMilliseconds = 1000 * 60 * 10;
        Dimension instanceDimension = new Dimension();
        instanceDimension.setName("InstanceId");
        List<Dimension> dims = new ArrayList<Dimension>();
        dims.add(instanceDimension);
        for (Instance instance : instancesTMP) {
            String name = instance.getInstanceId();
            String state = instance.getState().getName();
            Double avg = 0D;

            if (state.equals("running")) { 
                instanceDimension.setValue(name);
                GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime() - offsetInMilliseconds))
                .withNamespace("AWS/EC2")
                .withPeriod(60)
                .withMetricName("CPUUtilization")
                .withStatistics("Average")
                .withDimensions(instanceDimension)
                .withEndTime(new Date());
                GetMetricStatisticsResult getMetricStatisticsResult = 
                        cloudWatch.getMetricStatistics(request);
                List<Datapoint> datapoints = getMetricStatisticsResult.getDatapoints();

                for (Datapoint dp : datapoints) {
                    if(dp.getAverage() > avg)
                        avg = dp.getAverage();
                }

                instances.put(name, avg);
                lbal.addInstance(instance);
            } 
        }
    }

    @Data
    public class WaitForReqsToFinish implements Runnable {
        private final InstanceInfo instanceInfo;

        @Override
        public void run() {
            while (! instanceInfo.getCurrentRequests().isEmpty()) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
            String instanceId = instanceInfo.getInstanceData().getInstanceId();
            termInstanceReq.withInstanceIds();
            ec2.terminateInstances(termInstanceReq);
            instances.remove(instanceId);
            instanceToDelete = "none";
        }
    }


}
