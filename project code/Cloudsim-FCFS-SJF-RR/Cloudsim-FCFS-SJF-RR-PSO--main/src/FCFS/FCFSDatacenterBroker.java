package FCFS;


import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;

/**
 用于把一组任务按顺序分配给一组虚拟机，
 当所有的虚拟机都有运行任务后，再从第一个虚拟机从头分配任务
 */
public class FCFSDatacenterBroker extends DatacenterBroker {

    public FCFSDatacenterBroker(String name) throws Exception {
        super(name);
    }

    //调度函数
    public void scheduleTaskstoVms() {

        ArrayList<Cloudlet> clist = new ArrayList<Cloudlet>();//已经创建的任务列表

        for (Cloudlet cloudlet : getCloudletSubmittedList()) {//已经提交的任务列表
            clist.add(cloudlet);
        }

        setCloudletReceivedList(clist);//收到任务队列
    }

    @Override //方法重写
    protected void processCloudletReturn(SimEvent ev) { //云任务完成后返回到该函数
        Cloudlet cloudlet = (Cloudlet) ev.getData();//出队列，拿到待执行任务
        getCloudletReceivedList().add(cloudlet);
        Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
                + " received");
        cloudletsSubmitted--;
        if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) {//若当前还有任务
            scheduleTaskstoVms();//传递给虚拟机进行调度
            cloudletExecution(cloudlet);//任务开始执行
        }
    }


    protected void cloudletExecution(Cloudlet cloudlet) {

        if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // 任务队列为空，响应为空
            Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
            clearDatacenters();
            finishExecution();
        } else {
            if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {//有任务还未提交
                // 所有任务发送完毕，清空数据中心，在数据中心中创建虚拟机
                clearDatacenters();
                createVmsInDatacenter(0);
            }
        }
    }
}