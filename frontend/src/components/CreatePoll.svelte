<script>
    import { onMount } from 'svelte';
    import { API_BASE } from '../config.js';
    export let onDone;
    export let currentUser;
  
    let pollQuestion = '';
    let options = ['', ''];
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
      invitedUserIds = invitedUserIds;
    }
  
    function addOption() {
      options = [...options, ''];
    }
  
    function removeOption(index) {
      if (options.length > 2) {
        options = options.filter((_, i) => i !== index);
      }
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
          if (onDone) onDone();
        } else if (res.status === 400) {
          const errorText = await res.text();
          errors.general = errorText;
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
  <div class="form-group">
    <h3>Poll question</h3>
    <input type="text" bind:value={pollQuestion} placeholder="e.g., What's for lunch?" />
    {#if errors.question}<p class="error-message">{errors.question}</p>{/if}
  </div>

  <div class="form-group">
    <h3>Options</h3>
    {#each options as option, index}
      <div class="option-container">
        <input type="text" bind:value={options[index]} placeholder={`Option ${index + 1}`} />
        {#if options.length > 2}
          <button on:click={() => removeOption(index)} class="button button-danger button-sm">Remove</button>
        {/if}
      </div>
    {/each}
    <button on:click={addOption} class="button button-secondary">Add Option</button>
    {#if errors.options}<p class="error-message">{errors.options}</p>{/if}
  </div>

  <div class="form-group">
    <label style="display: inline-flex; align-items: center; gap: 0.5rem; cursor: pointer;">
      <input type="checkbox" bind:checked={isPublic} style="width:auto; margin:0;">
      Public Poll (anyone can vote)
    </label>
  </div>

  {#if !isPublic && currentUser}
    <div class="form-group">
      <h4>Invite users to vote:</h4>
      {#each allUsers as user (user.id)}
        {#if user.id !== currentUser.id}
          <label style="display:flex; align-items:center; gap:0.5rem; cursor: pointer;">
            <input type="checkbox" style="width:auto;"
              checked={invitedUserIds.has(user.id)}
              on:change={() => toggleInvite(user.id)} />
            {user.username}
          </label>
        {/if}
      {/each}
    </div>
  {/if}

  <button on:click={createPoll} class="button button-primary button-full-width" style="margin-top: var(--spacing-md);">Create Poll</button>

  {#if errors.general}
    <p class="error-message" style="text-align: center; margin-top: var(--spacing-lg);">{errors.general}</p>
  {/if}
</section>