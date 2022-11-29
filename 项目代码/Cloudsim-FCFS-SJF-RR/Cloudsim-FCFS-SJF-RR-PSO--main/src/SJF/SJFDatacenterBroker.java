package SJF;


import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.List;

public class SJFDatacenterBroker extends DatacenterBroker {

    public SJFDatacenterBroker(String name) throws Exception {
        super(name);
    }

    public void scheduleTaskstoVms() {

        Vm vm = vmList.get(0);//虚拟机0号
        ArrayList<Cloudlet> list = new ArrayList<Cloudlet>();
        for (Cloudlet cloudlet : getCloudletReceivedList()) {
            list.add(cloudlet);
        }

        Cloudlet[] list2 = list.toArray(new Cloudlet[list.size()]);//将任务队列转为数组
        Cloudlet temp = null;
        int n = list.size();

        for (int i = 0; i < n; i++) {//使用冒泡排序，将短作业放置前面
            for (int j = 1; j < (n - i); j++) {//根据任务长度进行比较
                if (list2[j - 1].getCloudletLength() > list2[j].getCloudletLength()  ) {
                    //交换任务队列顺序
                    temp = list2[j - 1];
                    list2[j - 1] = list2[j];
                    list2[j] = temp;
                }
            }
        }

        ArrayList<Cloudlet> list3 = new ArrayList<Cloudlet>();//使用List3用于存储list2元素，便于返回队列

        for (int i = 0; i < list2.length; i++) {
            list3.add(list2[i]);
        }

        setCloudletReceivedList(list3);
    }



    @Override
    protected void processCloudletReturn(SimEvent ev) {
        Cloudlet cloudlet = (Cloudlet) ev.getData();
        getCloudletReceivedList().add(cloudlet);
        Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
                + " received");
        cloudletsSubmitted--;
        if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) {
            scheduleTaskstoVms();//调度给虚拟机
            cloudletExecution(cloudlet);//任务进行执行
        }
    }

    protected void cloudletExecution(Cloudlet cloudlet) {

        if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // 所有任务都执行结束
            Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
            clearDatacenters();
            finishExecution();
        } else { // 当前还有任务未执行
            if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {

                clearDatacenters();
                createVmsInDatacenter(0);
            }
        }
    }


}