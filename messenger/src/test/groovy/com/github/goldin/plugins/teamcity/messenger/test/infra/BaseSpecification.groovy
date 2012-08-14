package com.github.goldin.plugins.teamcity.messenger.test.infra

import com.github.goldin.plugins.teamcity.messenger.api.*
import com.github.goldin.plugins.teamcity.messenger.api.Message.Urgency
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import java.security.SecureRandom


/**
 * Super class for all tests
 */
@ContextConfiguration( locations = 'classpath:/build-server-plugin-messenger-test.xml' )
class BaseSpecification extends Specification
{
    final   Random random       = new SecureRandom()
    final   File   messagesFile = new File ( Constants.MESSAGES_DIR, "${ Constants.PLUGIN_NAME }/messages.json" )

    @Autowired final MessagesContext       context
    @Autowired final MessagesConfiguration config
    @Autowired final MessagesUtil          util
    @Autowired final MessagesTable         messagesTable
    @Autowired final MessagesBean          messagesBean


    @Before
    void setup() { reset() }


    void reset ()
    {
        messagesBean.messagesTable.messages.clear()
        messagesBean.messagesTable.messageIdGenerator.set( 1000 )

        assert messagesBean.allMessages.isEmpty()

        messagesBean.usersTable.all.clear()
        messagesBean.usersTable.groups.clear()
        messagesBean.usersTable.users.clear()

        assert messagesBean.usersTable.messagesForAll.isEmpty()
        assert messagesBean.usersTable.groups.isEmpty()
        assert messagesBean.usersTable.users.isEmpty()
    }


    /**
     * Retrieves text of the file specified.
     * @param fileName name of the file to read
     * @return file text
     */
    @Requires({ fileName })
    @Ensures({ result })
    String text ( String fileName )
    {
        def    url = this.getClass().getResource( "/$fileName" )
        assert url, "Resource \"/$fileName\" not found"
        url.text
    }


    /**
     * Runs closure specified through all collection permutations, passing each permutation as a <code>List</code>.
     * @param c collection to iterate over
     * @param call closure to invoke for each collection permutation
     * @return number of permutations iterated
     */
    @Requires({ ( c != null ) && call })
    @Ensures({ result > 0 })
    int permutations ( Collection c, Closure call )
    {
        int counter = 0
        c.eachPermutation { List l -> counter++; call( l )}
        counter
    }


    /**
     * Runs closure specified through all permutations of collections specified, passing each pair of permutation
     * as two <code>List</code> arguments.
     * @param c1 first collection to iterate over
     * @param c2 second collection to iterate over
     * @param call closure to invoke for each permutation
     * @return number of permutations iterated
     */
    @Requires({ ( c1 != null ) && ( c2 != null ) && call })
    @Ensures({ result > 0 })
    int permutations ( Collection c1, Collection c2, Closure call )
    {
        int counter = 0
        c1.eachPermutation {
            List l1 -> c2.eachPermutation {
            List l2 -> counter++; call( l1, l2 ) }
        }
        counter
    }


    @SuppressWarnings([ 'PrivateFieldCouldBeFinal' ])
    private int    messageIdCounter = 1000

    /**
     * Retrieves test message with 'id' assigned.
     */
    @Requires({ urgency && ( sendToGroups != null ) && ( sendToUsers != null ) })
    @Ensures({ result.id > 0 })
    Message messageWithId ( Urgency urgency = Urgency.INFO, boolean sendToAll = true, List<String> sendToGroups = [], List<String> sendToUsers = [] )
    {
        new Message( messageIdCounter++, context, config, util, messageNoId( urgency, sendToAll, sendToGroups, sendToUsers ))
    }


    /**
     * Retrieves test message without 'id' assigned.
     */
    @Requires({ urgency && ( sendToGroups != null ) && ( sendToUsers != null ) })
    @Ensures({ result.id == -1 })
    Message messageNoId ( Urgency urgency = Urgency.INFO, boolean sendToAll = true, List<String> sendToGroups = [], List<String> sendToUsers = [] )
    {
        new Message( Constants.TEST_SENDER, urgency, "[$urgency] message", -1, sendToAll, sendToGroups, sendToUsers )
    }
}