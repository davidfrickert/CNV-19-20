package pt.ist.meic.cnv.webapp;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pt.ist.meic.cnv.webapp.LoadBalancer;
import pt.ist.meic.cnv.webapp.SudokuRestController;

@Component
@Order(value = 2)
public class ZScaler extends Thread {

    private static AmazonEC2 ec2;
    private static AmazonCloudWatch cloudWatch;
    private HashMap<String, Double> instances = new HashMap<>();
    private String instanceToDelete = "none";
    private Double maximumValue = 70D;
    private Double minimumValue = 30D;

    @Autowired
    private LoadBalancer lbal;

    public String getInstanceToDelete(){
        return instanceToDelete;
    }

    private static void init() throws Exception {

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

    public ZScaler(String initialID) throws Exception {
        init();
        instances.put(initialID, 0D);
    }

    public ZScaler() throws Exception{
        init();
        launchInstance();
        String initialID = instances.keySet().iterator().next();

        System.out.println("Autoscaler initialized.");
        System.out.println(lbal);
        instances.put(initialID, 0D);
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
            instancesTMP.addAll(reservation.getInstances());
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

        runInstancesRequest.withImageId("ami-0b458fe9058917a09")
                               .withInstanceType("t2.micro")
                               .withMinCount(1)
                               .withMaxCount(1)
                               .withKeyName("ssh-eu")
                               .withSecurityGroups("CNV");

        RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);

        String newInstanceId = runInstancesResult.getReservation().getInstances().get(0).getInstanceId();

        instances.put(newInstanceId, 0D);
    }

    /**
     * Terminates instances
     * @param instanceId
     */
    private void terminateInstance(String instanceId){
        if (instances.size() == 1 || instanceToDelete == "none") { return; }
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instanceId);
        ec2.terminateInstances(termInstanceReq);
        instances.remove(instanceId);

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
            System.out.println("lballlll " + lbal);
            Thread.sleep(60000);
            updateInstances();
            checkIfActionNeeded();
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
        Set<String> set = instances.keySet();
        Iterator<String> it = set.iterator();

        double smallestCPU = 100D;
        String tmpInstanceToDel = instanceToDelete;

        Double avg = 0D;
        String tmp = "none";
        
        for (Double e : instances.values()) {

            String inst = it.next();
            avg += e;
            //System.out.println(inst);
            //System.out.println(e);
            /*if (e > maximumValue) {
                //System.out.println(e + " > " + maximumValue);
                //launchInstance();
            } else */
            if (e < minimumValue && tmpInstanceToDel.equals("none") && e < smallestCPU) {
                
                tmp = inst;
                smallestCPU = e;
                //setInstanceToDelete(inst);
                //System.out.println(e + " < " + minimumValue);
                //terminateInstance(inst);
            }
        }

        avg /= instances.size();

        if (avg > maximumValue) {
            launchInstance();
        } else if(avg < minimumValue){
            setInstanceToDelete(tmp);
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
            } 
        }
    }

}
