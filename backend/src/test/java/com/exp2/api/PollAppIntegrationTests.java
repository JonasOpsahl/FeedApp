// package com.exp2.api;

// import com.exp2.api.service.InMemoryPollService;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.http.MediaType;
// import org.springframework.test.web.servlet.MockMvc;

// import static org.hamcrest.Matchers.is;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


//

// THESE TESTS ARE COMMENTED OUT AS THEY WILL FAIL IF THE PROFILE IS NOT THE IN-MEMORY ONE.

//


// @SpringBootTest
// @AutoConfigureMockMvc
// class PollAppIntegrationTests {

//     @Autowired
//     private MockMvc mockMvc; // The main tool for performing mock HTTP requests

//     @Autowired
//     private InMemoryPollService pollManager; // Needed to reset state

//     // This method runs before each test to ensure a clean slate
//     @BeforeEach
//     void setUp() {
//         pollManager.reset();
//     }

//     @Test
//     @DisplayName("POST /api/users - Should create a new user")
//     void shouldCreateUser() throws Exception {
//         mockMvc.perform(post("/api/users")
//                 .param("username", "testuser")
//                 .param("email", "test@example.com")
//                 .param("password", "password123"))
//                 .andExpect(status().isOk())
//                 .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                 .andExpect(jsonPath("$.username", is("testuser")))
//                 .andExpect(jsonPath("$.userId", is(1)));
//     }

//     @Test
//     @DisplayName("POST /api/polls - Should create a public poll")
//     void shouldCreatePublicPoll() throws Exception {
//         // First, create a user to be the poll creator
//         mockMvc.perform(post("/api/users").param("username", "creator").param("email", "c@c.com").param("password", "p"));

//         mockMvc.perform(post("/api/polls")
//                 .param("question", "Is this a public poll?")
//                 .param("durationDays", "7")
//                 .param("creatorId", "1")
//                 .param("visibility", "PUBLIC")
//                 .param("optionCaptions", "Yes", "No") // Pass list items as multiple params with the same key
//                 .param("optionOrders", "1,2"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.question", is("Is this a public poll?")))
//                 .andExpect(jsonPath("$.pollOptions[0].caption", is("Yes")));
//     }

//     @Test
//     @DisplayName("POST /api/polls/{id}/vote - Should allow anonymous vote on public poll")
//     void shouldAllowAnonymousVoteOnPublicPoll() throws Exception {
//         // 1. Create a user and a public poll
//         mockMvc.perform(post("/api/users").param("username", "creator").param("email", "c@c.com").param("password", "p"));
//         mockMvc.perform(post("/api/polls")
//                 .param("question", "Public Poll")
//                 .param("durationDays", "1")
//                 .param("creatorId", "1")
//                 .param("visibility", "PUBLIC")
//                 .param("optionCaptions", "Vote Here")
//                 .param("optionOrders", "10"));

//         // 2. Perform an anonymous vote on poll with ID 1, for option with order 10
//         mockMvc.perform(post("/api/polls/1/vote")
//                 .param("presentationOrder", "10")) // No userId is provided
//                 .andExpect(status().isOk())
//                 .andExpect(content().string("true"));
//     }

//     @Test
//     @DisplayName("POST /api/polls/{id}/vote - Should block user who has reached vote limit on private poll")
//     void shouldBlockUserAtVoteLimitOnPrivatePoll() throws Exception {
//         // 1. Create two users and a private poll with a limit of 1 vote per user
//         mockMvc.perform(post("/api/users").param("username", "creator").param("email", "c@c.com").param("password", "p"));
//         mockMvc.perform(post("/api/users").param("username", "voter").param("email", "v@v.com").param("password", "p"));
//         mockMvc.perform(post("/api/polls")
//                 .param("question", "Private Poll")
//                 .param("durationDays", "1")
//                 .param("creatorId", "1")
//                 .param("visibility", "PRIVATE")
//                 .param("maxVotesPerUser", "1")
//                 .param("invitedUsers", "1,2")
//                 .param("optionCaptions", "Option A")
//                 .param("optionOrders", "100"));

//         // 2. First vote (should succeed)
//         mockMvc.perform(post("/api/polls/1/vote")
//                 .param("presentationOrder", "100")
//                 .param("userId", "2"))
//                 .andExpect(status().isOk())
//                 .andExpect(content().string("true"));

//         // 3. Second vote (should fail)
//         mockMvc.perform(post("/api/polls/1/vote")
//                 .param("presentationOrder", "100")
//                 .param("userId", "2"))
//                 .andExpect(status().isOk())
//                 .andExpect(content().string("false"));
//     }

//     @Test
//     @DisplayName("GET /api/polls/{id}/results - Should return correct vote counts")
//     void shouldReturnCorrectPollResults() throws Exception {
//         // 1. Create users and a poll
//         mockMvc.perform(post("/api/users").param("username", "creator").param("email", "c@c.com").param("password", "p"));
//         mockMvc.perform(post("/api/users").param("username", "voter").param("email", "v@v.com").param("password", "p"));
//         mockMvc.perform(post("/api/polls")
//                 .param("question", "Results Poll")
//                 .param("durationDays", "1")
//                 .param("creatorId", "1")
//                 .param("visibility", "PUBLIC")
//                 .param("optionCaptions", "Yes", "No")
//                 .param("optionOrders", "1,2"));

//         // 2. Cast some votes
//         mockMvc.perform(post("/api/polls/1/vote").param("presentationOrder", "1").param("userId", "1")); // Yes
//         mockMvc.perform(post("/api/polls/1/vote").param("presentationOrder", "1").param("userId", "2")); // Yes
//         mockMvc.perform(post("/api/polls/1/vote").param("presentationOrder", "2"));                       // No (anonymous)

//         // 3. Check the results
//         mockMvc.perform(get("/api/polls/1/results"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.Yes", is(2)))
//                 .andExpect(jsonPath("$.No", is(1)));
//     }
// }