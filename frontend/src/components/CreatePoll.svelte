<script>
    import { onMount } from 'svelte';
    import { API_BASE } from '../config.js';
    export let onDone;
    export let currentUser;
  
    let pollQuestion = '';
    let options = ['', ''];
    let newOption = '';
    let isPublic = true;
    let allUsers = [];
    let invitedUserIds = new Set();
  
    let errors = {
      question: '',
      options: '',
      general: ''
    };
  
    onMount(async () => {
      try {
        const res = await fetch(`${API_BASE}/users`);
        allUsers = await res.json();
      } catch (err) {
        console.error(err);
        errors.general = `Network error: ${err.message}`;
      }
    });
  
    function toggleInvite(userId) {
      if (invitedUserIds.has(userId)) invitedUserIds.delete(userId);
      else invitedUserIds.add(userId);
    }
  
    function addOption() {
      if (newOption.trim() !== '') {
        options = [...options, newOption];
        newOption = '';
      }
    }
  
    function removeOption(index) {
      options = options.filter((_, i) => i !== index);
    }
  
    function validatePoll() {
      errors = { question: '', options: '', general: '' };
      if (!pollQuestion.trim()) errors.question = 'Poll question is required';
      if (options.filter(o => o.trim() !== '').length < 2) errors.options = 'Poll must have at least 2 options';
      return !errors.question && !errors.options;
    }
  
    async function createPoll() {
      if (!validatePoll()) return;
  
      const poll = {
        question: pollQuestion,
        options: options
          .filter(o => o.trim() !== '')
          .map((text, index) => ({ caption: text, presentationOrder: index })),
        public: isPublic,
        invitedUsers: Array.from(invitedUserIds).map(id => ({ id })),
        createdByUser: { id: currentUser.id }
      };
  
      try {
        const res = await fetch(`${API_BASE}/polls`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(poll)
        });
  
        if (res.ok) {
          pollQuestion = '';
          options = ['', ''];
          newOption = '';
          invitedUserIds.clear();
          errors = { question: '', options: '', general: '' };
          if (onDone) onDone();
        } else if (res.status === 400) {
          const errorText = await res.text();
          const msgs = errorText.split(';').map(s => s.trim());
          msgs.forEach(msg => {
            if (msg.toLowerCase().includes('question')) errors.question = msg;
            else if (msg.toLowerCase().includes('option')) errors.options = msg;
            else errors.general = msg;
          });
        } else {
          errors.general = `Unexpected error: ${res.statusText}`;
        }
      } catch (err) {
        console.error(err);
        errors.general = `Network error: ${err.message}`;
      }
    }
  </script>
  
  <section class="card">
    <h3>Poll title:</h3>
    <input type="text" bind:value={pollQuestion} placeholder="Enter poll title" />
    {#if errors.question}<p class="error">{errors.question}</p>{/if}
  
    <h3>Options:</h3>
    {#each options as option, index}
      <nav style="display:flex; align-items:center;">
        <input type="text" bind:value={options[index]} placeholder={`Option ${index + 1}`} />
        <button on:click={() => removeOption(index)} style="margin-left:0.5rem; margin-top:0.5rem;">Remove</button>
      </nav>
    {/each}
  
    <nav style="margin-bottom:1rem;">
      <input type="text" bind:value={newOption} placeholder="Add new option" />
      <button on:click={addOption} style="margin-left:0.5rem;">Add option</button>
    </nav>
    {#if errors.options}<p class="error">{errors.options}</p>{/if}
  
    <label style="display: inline-flex; align-items: center; gap: 0.5rem;">
      Public poll?
      <input type="checkbox" bind:checked={isPublic} style="outline:none; width:auto; margin:0;">
    </label>
  
    {#if !isPublic && currentUser}
      <h4>Invite users to vote:</h4>
      {#each allUsers as user (user.id)}
        {#if user.id !== currentUser.id}
          <label style="display:flex; align-items:center; gap:0.5rem; margin-bottom:0.3rem;">
            {user.username}
            <input type="checkbox" style="outline:none;"
              checked={invitedUserIds.has(user.id)}
              on:change={() => toggleInvite(user.id)} />
          </label>
        {/if}
      {/each}
    {/if}
  
    <nav>
      <button on:click={createPoll} style="margin-top:1rem;">Create poll</button>
    </nav>
  
    {#if errors.general}
      <p class="general">{errors.general}</p>
    {/if}
  </section>
  
  <style>
    .error { color: #ff5555; font-size: 0.9rem; margin-top: 0.25rem; }
    .general { color: #ffcc00; margin-top: 1rem; }
  </style>