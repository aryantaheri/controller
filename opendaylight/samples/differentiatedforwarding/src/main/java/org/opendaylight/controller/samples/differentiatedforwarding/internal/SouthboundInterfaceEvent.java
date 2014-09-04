package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.samples.differentiatedforwarding.SouthboundEvent;
import org.opendaylight.ovsdb.lib.notation.Row;

public class SouthboundInterfaceEvent extends SouthboundEvent{

    private Row oldRow;

    public SouthboundInterfaceEvent(Node node, String tableName, String uuid,
            Row row, Action action) {
        super(node, tableName, uuid, row, action);
    }

    public SouthboundInterfaceEvent(Node node, String tableName, String uuid,
            Row row, Object context, Action action) {
        super(node, tableName, uuid, row, context, action);
    }

    public Row getOldRow() {
        return oldRow;
    }

    public void setOldRow(Row oldRow) {
        this.oldRow = oldRow;
    }

    @Override
    public String toString() {
        return super.toString() + ", oldRow: " + oldRow;
    }
}
