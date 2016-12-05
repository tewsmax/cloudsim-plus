package org.cloudbus.cloudsim.examples;

import org.cloudsimplus.util.tablebuilder.CloudletsTableBuilderHelper;
import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.resources.Bandwidth;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Ram;

/**
 * An example showing how to create 1 host and place multiple VMs at the same
 * {@link Pe Processor Element (CPU core)} of it,
 * using a VmSchedulerTimeShared policy at the Host.
 *
 * It number of VMs to be created will be the double of the Host PEs number.
 * For each Host PE, two VMs requiring half the MIPS capacity of the PE will be
 * created. Each VM will have just one cloudlet that will use
 * all VM PEs and MIPS capacity.
 *
 * Thus, considering that each cloudlet has a length of 10000 MI and
 * each VM has a PE of 1000 MIPS, the cloudlet will spend 10 seconds to finish.
 * However, as each Host PE will be shared between two VMs using a time shared
 * scheduler, the cloudlet will spend the double of the time to finish,
 * as can be seen in the simulation results after running the example.
 *
 * @author Manoel Campos da Silva Filho
 */
public class SharingHostPEsUsingVmSchedulerTimeShared {
    /**
     * Capacity of each CPU core (in Million Instructions per Second).
     */
    private static final double HOST_MIPS = 1000;
    /**
     * Number of processor elements (CPU cores) of each host.
     */
    private static final int HOST_PES_NUM = 2;

    /**
     * The total MIPS capacity across all the Host PEs.
     */
    private static final double HOST_TOTAL_MIPS_CAPACITY = HOST_MIPS*HOST_PES_NUM;

    /**
     * The length of each created cloudlet in Million Instructions (MI).
     */
    private static final long CLOUDLET_LENGTH = 10000;

    /**
     * Number of VMs to create.
     */
    private static final int NUMBER_OF_VMS = HOST_PES_NUM*2;

    private static final double VM_MIPS = HOST_TOTAL_MIPS_CAPACITY/NUMBER_OF_VMS;
    private final CloudSim simulation;


    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;
    private int numberOfCreatedCloudlets = 0;
    private int numberOfCreatedVms = 0;
    private int numberOfCreatedHosts = 0;

    /**
     * Starts the simulation.
     * @param args
     */
    public static void main(String[] args) {
        new SharingHostPEsUsingVmSchedulerTimeShared();
    }

    /**
     * Default constructor where the simulation is built.
     */
    public SharingHostPEsUsingVmSchedulerTimeShared() {
        Log.printFormattedLine("Starting %s Example ...", getClass().getSimpleName());
        this.vmList = new ArrayList<>();
        this.cloudletList = new ArrayList<>();
        //Number of cloud customers
        int numberOfCloudUsers = 1;
        boolean traceEvents = false;

        simulation = new CloudSim(numberOfCloudUsers, traceEvents);

        Datacenter datacenter0 = createDatacenter();

        /*Creates a Broker accountable for submission of VMs and Cloudlets
        on behalf of a given cloud user (customer).*/
        DatacenterBroker broker0 = new DatacenterBrokerSimple(simulation);

        createAndSubmitVmsAndCloudlets(broker0);

        /*Starts the simulation and waits all cloudlets to be executed*/
        simulation.start();

        //Finishes the simulation
        simulation.stop();

        /*Prints results when the simulation is over
        (you can use your own code here to print what you want from this cloudlet list)*/
        List<Cloudlet> finishedCloudlets = broker0.getCloudletsFinishedList();
        new CloudletsTableBuilderHelper(finishedCloudlets).build();
        Log.printFormattedLine("%s Example finished!", getClass().getSimpleName());
    }

    private void createAndSubmitVmsAndCloudlets(DatacenterBroker broker0) {
        for(int i = 0; i < NUMBER_OF_VMS; i++){
            Vm vm = createVm(broker0, VM_MIPS, 1);
            this.vmList.add(vm);

            /*Creates a cloudlet that represents an application to be run inside a VM.*/
            Cloudlet cloudlet = createCloudlet(broker0, vm);
            this.cloudletList.add(cloudlet);
        }

        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);
    }

    private DatacenterSimple createDatacenter() {
        List<Host> hostList = new ArrayList<>();
        Host host0 = createHost();
        hostList.add(host0);

        //Defines the characteristics of the data center
        double cost = 3.0; // the cost of using processing in this switches
        double costPerMem = 0.05; // the cost of using memory in this switches
        double costPerStorage = 0.001; // the cost of using storage in this switches
        double costPerBw = 0.0; // the cost of using bw in this switches

        DatacenterCharacteristics characteristics =
            new DatacenterCharacteristicsSimple(hostList)
                .setCostPerSecond(cost)
                .setCostPerMem(costPerMem)
                .setCostPerStorage(costPerStorage)
                .setCostPerBw(costPerBw);

        return new DatacenterSimple(simulation, characteristics, new VmAllocationPolicySimple());
    }

    private Host createHost() {
        long ram = 2048; // host memory (MB)
        long storage = 1000000; // host storage (MB)
        long bw = 10000; //Megabits/s

        List<Pe> peList = new ArrayList<>();
        /*Creates the Host's CPU cores and defines the provisioner
        used to allocate each core for requesting VMs.*/
        for(int i = 0; i < HOST_PES_NUM; i++){
            peList.add(new PeSimple(i, new PeProvisionerSimple(HOST_MIPS)));
        }

        return new HostSimple(numberOfCreatedHosts++, storage, peList)
            .setRamProvisioner(new ResourceProvisionerSimple(new Ram(ram)))
            .setBwProvisioner(new ResourceProvisionerSimple(new Bandwidth(bw)))
            .setVmScheduler(new VmSchedulerTimeShared());
    }

    private Vm createVm(DatacenterBroker broker, double mips, int pesNumber) {
        long storage = 10000; // vm image size (MB)
        int  ram = 512; // vm memory (MB)
        long bw = 1000; // vm bandwidth

        return new VmSimple(numberOfCreatedVms++, mips, pesNumber)
            .setRam(ram).setBw(bw).setSize(storage)
            .setCloudletScheduler(new CloudletSchedulerTimeShared())
            .setBroker(broker);

    }

    private Cloudlet createCloudlet(DatacenterBroker broker, Vm vm) {
        long fileSize = 300; //Size (in bytes) before execution
        long outputSize = 300; //Size (in bytes) after execution
        int  numberOfCpuCores = vm.getNumberOfPes(); //cloudlet will use all the VM's CPU cores

        //Defines how CPU, RAM and Bandwidth resources are used
        //Sets the same utilization model for all these resources.
        UtilizationModel utilization = new UtilizationModelFull();

        Cloudlet cloudlet
            = new CloudletSimple(numberOfCreatedCloudlets++, CLOUDLET_LENGTH, numberOfCpuCores)
                .setCloudletFileSize(fileSize)
                .setCloudletOutputSize(outputSize)
                .setUtilizationModel(utilization)
                .setBroker(broker)
                .setVm(vm);

        return cloudlet;
    }

}