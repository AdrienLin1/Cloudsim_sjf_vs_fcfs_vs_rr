package SJF;


import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import utils.Constants;
import utils.DatacenterCreator;
import utils.GenerateMatrices;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class SJF_Scheduler {

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

        //创建虚拟机队列
        Vm[] vm = new Vm[vms];

        for (int i = 0; i < vms; i++) {
            vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }

        return list;
    }

    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
        // 任务队列
        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();


        //任务参数
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet[] cloudlet = new Cloudlet[cloudlets];

        for (int i = 0; i < cloudlets; i++) {
            int dcId = (int) (Math.random() * Constants.NO_OF_DATA_CENTERS);
            long length = (long) (1e3 * (commMatrix[i][dcId] + execMatrix[i][dcId]));//

            cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);

            cloudlet[i].setUserId(userId);
            cloudlet[i].setVmId(dcId + 2);
            list.add(cloudlet[i]);
        }
        return list;
    }

    public static void main(String[] args) {
        Log.printLine("Starting SJF Scheduler...");

        new GenerateMatrices();
        execMatrix = GenerateMatrices.getExecMatrix();
        commMatrix = GenerateMatrices.getCommMatrix();

        try {
            int num_user = 1;   // 用户代理
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // 粒度小的注释

            CloudSim.init(num_user, calendar, trace_flag);

            // 建数据中心
            datacenter = new Datacenter[Constants.NO_OF_DATA_CENTERS];
            for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
                datacenter[i] = DatacenterCreator.createDatacenter("Datacenter_" + i);
            }

            //短作业优先的用户代理
            SJFDatacenterBroker broker = createBroker("Broker_0");
            int brokerId = broker.getId();

            //创建虚拟机列表和任务列表
            vmList = createVM(brokerId, Constants.NO_OF_DATA_CENTERS);
            cloudletList = createCloudlet(brokerId, Constants.NO_OF_TASKS, 0);

            broker.submitVmList(vmList);//虚拟机绑定用户代理
            broker.submitCloudletList(cloudletList);//云端任务绑定用户代理

            // 开始模拟
            CloudSim.startSimulation();

            // 云端收到待处理队列
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            //停止模拟
            CloudSim.stopSimulation();
            //输出结果
            printCloudletList(newList);

            Log.printLine(SJF_Scheduler.class.getName() + " finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static SJFDatacenterBroker createBroker(String name) throws Exception {
        return new SJFDatacenterBroker(name);
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
                //云计算每秒的费用*实际占用CPU时间

                totalCompletionTime += completionTime;
                totalCost += cost;
                totalWaitingTime += cloudlet.getWaitingTime();
                totalCpuTime+=cloudlet.getActualCPUTime();

            }
        }
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + dft.format(cloudlet.getCloudletId()) + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");

                //HERE:
                double completionTime= cloudlet.getActualCPUTime()+ cloudlet.getWaitingTime();
                double cost= cloudlet.getCostPerSec()* cloudlet.getActualCPUTime() ;
                //云计算每秒的费用*实际占用CPU时间
                double cpuTime=cloudlet.getActualCPUTime();
                stdTime+=(cpuTime-totalCpuTime/size)*(cpuTime-totalCpuTime/size);//引入均方差
                //每一个任务单元的执行时间-平均时间再平方
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

        //Added:
        Log.printLine("Total Completion Time: " + totalCompletionTime +" Avg Completion Time: "+ (totalCompletionTime/20));
        Log.printLine("Total Cost : " + totalCost+ " Avg cost: "+ (totalCost/20));
        Log.printLine("Avg Waiting Time: "+ (totalWaitingTime/20));
        Log.printLine("standard Deviation Time: "+ (Math.sqrt(stdTime/20)));
    }


}
