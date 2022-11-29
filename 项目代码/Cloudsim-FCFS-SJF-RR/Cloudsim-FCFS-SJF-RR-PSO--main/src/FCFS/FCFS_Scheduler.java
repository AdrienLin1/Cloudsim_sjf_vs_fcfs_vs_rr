package FCFS;
import RoundRobin.RoundRobinDatacenterBroker;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import utils.Constants;
import utils.DatacenterCreator;
import utils.GenerateMatrices;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class FCFS_Scheduler {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;
    private static Datacenter[] datacenter;
    private static double[][] commMatrix;//commMatrix是云任务传到broker绑定所花的时间
    private static double[][] execMatrix;//任务本身运行的时间

    private static List<Vm> createVM(int userId, int vms) {
        //创建虚拟机列表，后传送给用户代理
        LinkedList<Vm> list = new LinkedList<Vm>();

        //虚拟机参数
        long size = 10000; //image size (MB)
        int ram = 512; //虚拟机内存
        int mips = 1000;//虚拟机每秒执行指令数
        long bw = 1000;//带宽
        int pesNumber = 1; //cpu数量
        String vmm = "肥崽"; //虚拟机名称

        //创建虚拟机
        Vm[] vm = new Vm[vms];

        for (int i = 0; i < vms; i++) {//虚拟机绑定数据中心
            vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }

        return list;
    }

    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
        // 产生保存云端任务列表的容器
        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

        //云端任务参数
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet[] cloudlet = new Cloudlet[cloudlets];

        for (int i = 0; i < cloudlets; i++) {
            int dcId = (int) (Math.random() * Constants.NO_OF_DATA_CENTERS);//随机分数据中心的id
           long length = (long) (1e3 * (commMatrix[i][dcId] + execMatrix[i][dcId]));//随机分云任务长度（上传到broker绑定时间+任务本身运行时间）
            cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            // 设置任务所分配的用户id和虚拟机id
            cloudlet[i].setUserId(userId);
            cloudlet[i].setVmId(dcId + 2);
            list.add(cloudlet[i]);
        }
        return list;//将云端任务返回
    }

    public static void main(String[] args) {
        Log.printLine("Starting FCFS Scheduler...");

        new GenerateMatrices();
        execMatrix = GenerateMatrices.getExecMatrix();
        commMatrix = GenerateMatrices.getCommMatrix();

        try {
            int num_user = 1;   // 用户数
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // false表示不需要粒度过小的输出日志文件信息

            CloudSim.init(num_user, calendar, trace_flag);//云端库进行初始化

            // 产生数据中心
            datacenter = new Datacenter[Constants.NO_OF_DATA_CENTERS];
            for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
                datacenter[i] = DatacenterCreator.createDatacenter("Datacenter_" + i);
            }

            //产生用户代理 1个
            FCFSDatacenterBroker broker = createBroker("Broker_0");
            int brokerId = broker.getId();

            //创建虚拟机和任务集，并交给用户代理
            vmList = createVM(brokerId, Constants.NO_OF_VMS);
            cloudletList = createCloudlet(brokerId, Constants.NO_OF_TASKS, 0);

            broker.submitVmList(vmList);//虚拟机队列绑定用户代理
            broker.submitCloudletList(cloudletList);//任务队列绑定用户代理，直接跳到FCFSDatacenterBroker中的scheduler调度

            // 云中心开始模拟
            CloudSim.startSimulation();

            // 用户代理收到云端待执行任务队列
            List<Cloudlet> newList = broker.getCloudletReceivedList();

            //云中心模拟结束
            CloudSim.stopSimulation();

            //打印云任务调度和评估结果
            printCloudletList(newList);

            Log.printLine(FCFS_Scheduler.class.getName() + " finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("模拟失败");
        }

    }

    private static FCFSDatacenterBroker createBroker(String name) throws Exception {
        return new FCFSDatacenterBroker(name);//初始化FCFS的数据中心
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
                indent +  "Time" +
                indent + "Start Time" +
                indent +  "Finish Time"+
                indent + "WaitingTime"+
                indent + "CompletionTime"+
                indent + "Cost");

        //HERE:
        double totalCompletionTime=0;//总完成时间
        double totalCost=0;//总消耗
        double totalWaitingTime=0;//所有任务等待时间
        double stdTime=0;//均方差
        double totalCpuTime=0;//所有任务占用CPU时间
        //-------------------------
        
        DecimalFormat dft = new DecimalFormat("####.##");
        dft.setMinimumIntegerDigits(2);
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {

                double completionTime= cloudlet.getActualCPUTime()+ cloudlet.getWaitingTime();//计算完成时间
                double cost= cloudlet.getCostPerSec()* cloudlet.getActualCPUTime() ;//计算每秒钟CPU消耗

                totalCompletionTime += completionTime;
                totalCost += cost;
                totalWaitingTime+=cloudlet.getWaitingTime();
                totalCpuTime+=cloudlet.getActualCPUTime();//CPU执行时间
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
                //引入均方差来评判FCFS虚拟机调度负载均衡率
                Log.printLine(indent + indent + dft.format(cloudlet.getResourceId()) +
                        indent + indent + indent+ dft.format(cloudlet.getVmId()) +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + dft.format(cloudlet.getFinishTime()) +
                        indent + indent + dft.format(cloudlet.getWaitingTime()) +
                        indent + indent + dft.format(completionTime) +
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
