package com.exp2.api;

import com.exp2.api.model.Poll;
import com.exp2.api.model.User;
import com.exp2.api.model.VoteOption;
import com.exp2.api.service.InMemoryPollService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PollAppUnitTests {

    private InMemoryPollService pollManager;
    private User user1;
    private User user2;

    // This method runs before each test, ensuring a clean state
    @BeforeEach
    void setUp() {
        pollManager = new InMemoryPollService();
        user1 = pollManager.createUser("Alice", "alice@example.com", "pass123");
        user2 = pollManager.createUser("Bob", "bob@example.com", "pass456");
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserTests {
        @Test
        @DisplayName("Should create a user successfully")
        void createUser() {
            assertNotNull(user1);
            assertEquals("Alice", user1.getUsername());
            assertEquals(1, user1.getUserId());
        }

        @Test
        @DisplayName("Should get a list of all users")
        void getUsers() {
            assertEquals(2, pollManager.getUsers().size());
        }
        
        @Test
        @DisplayName("Should update a user's details")
        void updateUser() {
            pollManager.updateUser(user1.getUserId(), Optional.of("Alicia"), Optional.empty(), Optional.empty());
            assertEquals("Alicia", pollManager.getUser(user1.getUserId()).getUsername());
        }

        @Test
        @DisplayName("Should delete a user")
        void deleteUser() {
            assertTrue(pollManager.deleteUser(user2.getUserId()));
            assertNull(pollManager.getUser(user2.getUserId()));
            assertEquals(1, pollManager.getUsers().size());
        }
    }

    @Nested
    @DisplayName("Voting on a Public Poll")
    class PublicPollTests {
        private Poll publicPoll;

        @BeforeEach
        void createPublicPoll() {
            VoteOption option1 = new VoteOption();
            option1.setCaption("Yes");
            option1.setPresentationOrder(1);
            VoteOption option2 = new VoteOption();
            option2.setCaption("No");
            option2.setPresentationOrder(2);
            
            publicPoll = pollManager.createPoll(
                "Is Java fun?", 1, user1.getUserId(), Poll.Visibility.PUBLIC,
                Optional.empty(), List.of(), List.of(option1, option2)
            );
        }

        @Test
        @DisplayName("An authenticated user should be able to vote multiple times")
        void authenticatedUserCanVoteMultipleTimes() {
            assertTrue(pollManager.castVote(publicPoll.getPollId(), Optional.of(user1.getUserId()), 1));
            assertTrue(pollManager.castVote(publicPoll.getPollId(), Optional.of(user1.getUserId()), 2));
            
            Map<String, Integer> results = pollManager.getPollResults(publicPoll.getPollId());
            assertEquals(1, results.get("Yes"));
            assertEquals(1, results.get("No"));
        }

        @Test
        @DisplayName("An anonymous user should be able to vote")
        void anonymousUserCanVote() {
            assertTrue(pollManager.castVote(publicPoll.getPollId(), Optional.empty(), 1));
            
            Map<String, Integer> results = pollManager.getPollResults(publicPoll.getPollId());
            assertEquals(1, results.get("Yes"));
            assertEquals(0, results.get("No"));
        }
    }
    
    @Nested
    @DisplayName("Voting on a Private Poll")
    class PrivatePollTests {
        private Poll privatePoll;

        @BeforeEach
        void createPrivatePoll() {
            VoteOption optionA = new VoteOption();
            optionA.setCaption("A");
            optionA.setPresentationOrder(10);
            VoteOption optionB = new VoteOption();
            optionB.setCaption("B");
            optionB.setPresentationOrder(20);

            privatePoll = pollManager.createPoll(
                "Choose A or B?", 1, user1.getUserId(), Poll.Visibility.PRIVATE,
                Optional.of(2), // maxVotesPerUser = 2
                new ArrayList<>(List.of(user1.getUserId(), user2.getUserId())), List.of(optionA, optionB)
            );
        }
        
        @Test
        @DisplayName("An invited user should be able to vote up to their limit")
        void invitedUserCanVoteUpToLimit() {
            assertTrue(pollManager.castVote(privatePoll.getPollId(), Optional.of(user2.getUserId()), 10));
            assertTrue(pollManager.castVote(privatePoll.getPollId(), Optional.of(user2.getUserId()), 20));
        }

        @Test
        @DisplayName("An invited user should be blocked after reaching their vote limit")
        void invitedUserBlockedAfterLimit() {
            pollManager.castVote(privatePoll.getPollId(), Optional.of(user2.getUserId()), 10);
            pollManager.castVote(privatePoll.getPollId(), Optional.of(user2.getUserId()), 20);
            
            // This third vote should fail
            assertFalse(pollManager.castVote(privatePoll.getPollId(), Optional.of(user2.getUserId()), 10));
        }

        @Test
        @DisplayName("An anonymous user should be blocked from voting")
        void anonymousUserBlocked() {
            assertFalse(pollManager.castVote(privatePoll.getPollId(), Optional.empty(), 10));
        }
        
        @Test
        @DisplayName("A non-invited user should be blocked from voting")
        void nonInvitedUserBlocked() {
            User user3 = pollManager.createUser("Charlie", "charlie@example.com", "pass789");
            assertFalse(pollManager.castVote(privatePoll.getPollId(), Optional.of(user3.getUserId()), 10));
        }
    }
    
    @Test
    @DisplayName("Should return correct results for a poll with votes")
    void getPollResults() {
        // Arrange
        VoteOption op1 = new VoteOption();
        op1.setCaption("Yes");
        op1.setPresentationOrder(1);
        VoteOption op2 = new VoteOption();
        op2.setCaption("No");
        op2.setPresentationOrder(2);
        Poll poll = pollManager.createPoll("Test Poll", 1, user1.getUserId(), Poll.Visibility.PUBLIC, Optional.empty(), List.of(), List.of(op1, op2));

        // Act
        pollManager.castVote(poll.getPollId(), Optional.of(user1.getUserId()), 1);
        pollManager.castVote(poll.getPollId(), Optional.of(user2.getUserId()), 1);
        pollManager.castVote(poll.getPollId(), Optional.empty(), 2);

        // Assert
        Map<String, Integer> results = pollManager.getPollResults(poll.getPollId());
        assertEquals(2, results.get("Yes"));
        assertEquals(1, results.get("No"));
    }
}