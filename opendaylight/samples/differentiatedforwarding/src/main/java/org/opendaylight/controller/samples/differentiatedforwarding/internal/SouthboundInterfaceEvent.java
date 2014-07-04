package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.neutron.SouthboundEvent;

public class SouthboundInterfaceEvent extends SouthboundEvent{

    private Table<?> oldRow;

    public SouthboundInterfaceEvent(Node node, String tableName, String uuid,
            Table<?> row, Action action) {
        super(node, tableName, uuid, row, action);
    }

    public SouthboundInterfaceEvent(Node node, String tableName, String uuid,
            Table<?> row, Object context, Action action) {
        super(node, tableName, uuid, row, context, action);
    }

    public Table<?> getOldRow() {
        return oldRow;
    }

    public void setOldRow(Table<?> oldRow) {
        this.oldRow = oldRow;
    }

    @Override
    public String toString() {
        return super.toString() + ", oldRow: " + oldRow;
    }
}
