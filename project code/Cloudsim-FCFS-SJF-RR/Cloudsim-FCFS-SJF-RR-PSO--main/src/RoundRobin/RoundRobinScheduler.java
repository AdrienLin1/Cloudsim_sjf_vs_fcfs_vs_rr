package RoundRobin;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import utils.Constants;
import utils.DatacenterCreator;
import utils.GenerateMatrices;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;


public class RoundRobinScheduler {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;
    private static Datacenter[] datacenter;
    private static double[][] commMatrix;
    private static double[][] execMatrix;

    private static List<Vm> createVM(int userId, int vms) {
        //虚拟机队列
        LinkedList<Vm> list = new LinkedList<Vm>();
        //虚拟机参数
        long size = 10000;
        int ram = 512;
        int mips = 1000;
        long bw = 1000;
        int pesNumber = 1;
        String vmm = "肥崽";

        //虚拟机初始化
        Vm[] vm = new Vm[vms];
        for (int i = 0; i < vms; i++) {
            vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }

        return list;
    }

    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
        // 云任务队列
        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();


        
        //每个任务参数
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;//需要占用CPU数目
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet[] cloudlet = new Cloudlet[cloudlets];

        for (int i = 0; i < cloudlets; i++) {
            int dcId = (int) (Math.random() * Constants.NO_OF_DATA_CENTERS);

            long length = (long) (1e3 * (commMatrix[i][dcId] + execMatrix[i][dcId]));

            cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet[i].setUserId(userId);
            cloudlet[i].setVmId(dcId + 2);
            list.add(cloudlet[i]);
        }
        return list;
    }

    public static void main(String[] args) {
        Log.printLine("Starting Round Robin Scheduler...");

        new GenerateMatrices();
        execMatrix = GenerateMatrices.getExecMatrix();
        commMatrix = GenerateMatrices.getCommMatrix();

        try {
            int num_user = 1;   // 用户数
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            // 默认数据中心
            datacenter = new Datacenter[Constants.NO_OF_DATA_CENTERS];
            for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
                datacenter[i] = DatacenterCreator.createDatacenter("Datacenter_" + i);
            }

            //默认轮询代理
            RoundRobinDatacenterBroker broker = createBroker("Broker_0");
            int brokerId = broker.getId();

            //建虚拟机列表和云任务列表
            vmList = createVM(brokerId, Constants.NO_OF_DATA_CENTERS);
            cloudletList = createCloudlet(brokerId, Constants.NO_OF_TASKS, 0);
            //分别进行绑定
            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            //开始模拟
            CloudSim.startSimulation();

            //收到上载的云端队列
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            //模拟结束
            CloudSim.stopSimulation();
            //输出结果
            printCloudletList(newList);

            Log.printLine(RoundRobinScheduler.class.getName() + " finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static RoundRobinDatacenterBroker createBroker(String name) throws Exception {
        return new RoundRobinDatacenterBroker(name);
    }


    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" +
                indent + "Data center ID" +
                indent + "VM ID" +
                indent + indent + "Time" +
                indent + indent+ "Start Time" +
                indent + indent+ indent+"Finish Time"+
                indent + "WaitingTime"+
                indent + "CompletionTime"+
                indent + "Cost");

        //HERE:
        double totalCompletionTime=0;
        double totalCost=0;
        double totalWaitingTime=0;
        double stdTime=0;
        double totalCpuTime=0;
        //-------------------------
        
        DecimalFormat dft = new DecimalFormat("####.##");
        dft.setMinimumIntegerDigits(2);
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                double completionTime= cloudlet.getActualCPUTime()+ cloudlet.getWaitingTime();
                double cost= cloudlet.getCostPerSec()* cloudlet.getActualCPUTime() ;

                totalCompletionTime += completionTime;
                totalCost += cost;
                totalWaitingTime+=cloudlet.getWaitingTime();
                totalCpuTime+=cloudlet.getActualCPUTime();
                //-----------------------------------------
            }
        }
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + dft.format(cloudlet.getCloudletId()) + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");


                double completionTime= cloudlet.getActualCPUTime()+ cloudlet.getWaitingTime();
                double cost= cloudlet.getCostPerSec()* cloudlet.getActualCPUTime() ;
                double cpuTime=cloudlet.getActualCPUTime();
                stdTime+=(cpuTime-totalCpuTime/size)*(cpuTime-totalCpuTime/size);

                Log.printLine(indent + indent + dft.format(cloudlet.getResourceId()) +
                        indent + indent + indent + dft.format(cloudlet.getVmId()) +
                        indent + indent +indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent  +indent+indent+ dft.format(cloudlet.getFinishTime())+
                        indent + indent  +indent+ dft.format(cloudlet.getWaitingTime() )+
                        indent + indent  + dft.format(completionTime )+
                        indent + indent + dft.format(cost));
            }
        }

        Log.printLine("Total Completion Time: " + totalCompletionTime +" Avg Completion Time: "+ (totalCompletionTime/20));
        Log.printLine("Total Cost : " + totalCost+ " Avg cost: "+ (totalCost/20));
        Log.printLine("Avg Waiting Time: "+ (totalWaitingTime/20));
        Log.printLine("standard Deviation Time: "+ (Math.sqrt(stdTime/20)));
        
    }


}