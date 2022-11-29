package utils;


import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DatacenterCreator {

    public static Datacenter createDatacenter(String name) {


        // 多个物理机器（云端PC）列表
        List<Host> hostList = new ArrayList<Host>();

        //    一个物理机器上所存在的部署CPU数量
        List<Pe> peList = new ArrayList<Pe>();

        int mips = 1000;//每秒处理百万条指令数
        // 将CPU加入列表中
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        //4. 绑定主机id和CPU
        int hostId = 0;
        int ram = 2048; //主机内存大小
        long storage = 1000000; //主机主存大小
        int bw = 200000;//带宽

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList,
                        new VmSchedulerTimeShared(peList)
                )
        ); // 第一台部署的机器

        // 5. 数据中心的属性，采用linux x86架构
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;         // 时区
        double cost = 3.0;              // 使用资源的成本
        double costPerMem = 0.05;        // 使用内存的成本
        double costPerStorage = 0.1;    // 使用外存的成本
        double costPerBw = 0.1;
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datacenter;
    }
}
