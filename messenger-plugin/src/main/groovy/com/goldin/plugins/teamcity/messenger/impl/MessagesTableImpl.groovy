package com.goldin.plugins.teamcity.messenger.impl

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import com.goldin.plugins.teamcity.messenger.api.*
import org.gcontracts.annotations.Invariant


/**
 * {@link MessagesTable} implementation
 */
@Invariant({ this.configuration && this.context && this.util &&
             ( this.messages != null ) && ( this.messageIdGenerator != null )})
class MessagesTableImpl implements MessagesTable
{
    private final MessagesConfiguration configuration
    private final MessagesContext       context
    private final MessagesUtil          util
    private final Map<Long, Message>    messages
    private final AtomicLong            messageIdGenerator


    @Requires({ configuration && context && util })
    MessagesTableImpl ( MessagesConfiguration configuration, MessagesContext context, MessagesUtil util )
    {
        this.configuration      = configuration
        this.context            = context
        this.util               = util
        this.messages           = new ConcurrentHashMap( 128, 0.75f, 10 )
        this.messageIdGenerator = new AtomicLong( 1000 )
    }


    /**
     * Generates new message id.
     * @return new message id
     */
    @Ensures({ ( result > 0 ) && ( ! messages.containsKey( result )) })
    private long getNextMessageId () { messageIdGenerator.incrementAndGet() }


    @Override
    @Requires({ message.usersDeleted.isEmpty() })
    Message addMessage ( Message message )
    {
        long    messageId  = nextMessageId
        Message newMessage = new Message( messageId, context, util, message )
        Message previous   = messages.put( messageId, newMessage )
        assert  previous  == null, "Message with new id [$messageId] already existed: [$previous]"

        newMessage
    }


    @Override
    @Requires({ messages.containsKey( messageId ) })
    Message getMessage ( long messageId )
    {
        messages[ messageId ]
    }


    @Override
    @Requires({  messages.containsKey( messageId ) })
    @Ensures({ ! messages.containsKey( messageId ) })
    Message deleteMessage ( long messageId )
    {
        messages.remove( messageId )
    }


    @Override
    @Requires({ messages.containsKey( messageId ) && username })
    @Ensures({ result.usersDeleted.contains( username ) })
    Message deleteMessageByUser ( long messageId, String username )
    {
        Message m = messages[ messageId ]
        assert  m.forUser( username ), "[$m] is not for user [$username], can not be deleted by him"

        m.usersDeleted << username
        if (( ! m.sendToAll ) && ( ! m.sendToGroups ) && ( m.usersDeleted.containsAll( m.sendToUsers )))
        {
            deleteMessage( m.id )
        }
        m
    }


    @Override
    void deleteAllMessages ()
    {
        messages.clear()
    }


    @Override
    List<Message> getAllMessages ()
    {
        new ArrayList<Message>( messages.values())
    }


    @Override
    boolean containsMessage ( long messageId )
    {
        messages.containsKey( messageId )
    }


    @Override
    int getNumberOfMessages ()
    {
        messages.size()
    }


    Map getPersistencyData()
    {
        [ messageId : messageIdGenerator.get(),
          messages  : allMessages*.messagePersistencyData ] // List of Maps, one Map per Message
    }


    @Override
    @Requires({ data.isEmpty() || ( data[ 'messageId' ] && ( data[ 'messages' ] != null )) })
    void readPersistencyData( Map data )
    {
        if ( data )
        {
            messageIdGenerator.set( data[ 'messageId' ] as long )

            for ( Map messagePersistencyData in data[ 'messages' ] )
            {
                long      messageId   = messagePersistencyData[ 'id' ] as long
                messages[ messageId ] = new Message( messagePersistencyData, context, util )
            }
        }
    }
}
