package RoundRobin;


import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.List;

public class RoundRobinDatacenterBroker extends DatacenterBroker {

    public RoundRobinDatacenterBroker(String name) throws Exception {
        super(name);//继承并初始化基于3.03的CloudSim中 DatacenterBroker父类
    }

    @Override
    protected void processResourceCharacteristics(SimEvent ev) {
        //处理从datacenter中心返回的请求characteristic事件
        DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
        ////获得datacenter的characteristic
        getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);
        //添加到datacenter和characteristic映射表中

        //每一个datacenter都得到了characteristic
        if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
            distributeRequestsForNewVmsAcrossDatacentersUsingTheRoundRobinApproach();
            ////向数据中心请求建立vm
        }
    }

    /**
     * 是提交了任务列表和Vm列表，并未将任务分配给指定的虚拟机，那么Datacenterbroker将会，
     * 检测当前有未使用的虚拟机，检测有未分配的任务，使用顺序的轮转法进行分配
     */
    protected void distributeRequestsForNewVmsAcrossDatacentersUsingTheRoundRobinApproach() {
        int numberOfVmsAllocated = 0;
        int i = 0;

        final List<Integer> availableDatacenters = getDatacenterIdsList();

        for (Vm vm : getVmList()) {
            int datacenterId = availableDatacenters.get(i++ % availableDatacenters.size());
            //获得可用数据中心的id
            String datacenterName = CloudSim.getEntityName(datacenterId);//调用函数获得其名称

            if (!getVmsToDatacentersMap().containsKey(vm.getId())) {//到映射表中寻找
                Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId() + " in " + datacenterName);
                sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
                numberOfVmsAllocated++;
            }
        }

        setVmsRequested(numberOfVmsAllocated);
        setVmsAcks(0);
    }
}