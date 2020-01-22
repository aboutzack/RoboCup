package AIT_2019.module.complex;

import adf.component.communication.CommunicationMessage;
import rescuecore2.worldmodel.EntityID;
import es.csic.iiia.bms.*;
import java.util.*;
import static java.util.stream.Collectors.*;

public class BufferedCommunicationAdapter
    implements CommunicationAdapter<EntityID>
{
    private List<Message> buffer = new LinkedList<>();

    @Override
    public void send(double message, EntityID sender, EntityID recipient)
    {
        this.buffer.add(new Message(sender, recipient, message));
    }

    public void execute(Map<EntityID, Factor<EntityID>> nodes)
    {
        final List<Message> tmp = this.buffer;
        this.buffer = new LinkedList<>();

        for (Message message : tmp)
        {
            final Factor<EntityID> node = nodes.get(message.recipient);
            if (!node.getNeighbors().contains(message.sender)) continue;
            node.receive(message.value, message.sender);
        }
    }

    private class Message
    {
        public final double value;
        public final EntityID sender;
        public final EntityID recipient;

        public Message(EntityID sender, EntityID recipient, double value)
        {
            this.sender = sender;
            this.recipient = recipient;
            this.value = value;
        }
    }
}
