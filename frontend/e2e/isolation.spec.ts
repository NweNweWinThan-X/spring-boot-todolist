import { expect, test } from '@playwright/test';
import { quickAdd, register, uniqueAccount } from './helpers';

test.describe('Data isolation', () => {
  test('one account cannot see or reach another account\'s tasks', async ({ page }) => {
    const alice = uniqueAccount();
    await register(page, alice);
    await quickAdd(page, "Alice's private task");

    const taskId = await page.getByTestId('task-card').first().getAttribute('data-task-id');
    expect(taskId).toBeTruthy();

    await page.getByTestId('logout').click();
    await page.waitForURL(/\/login$/);

    const bob = uniqueAccount();
    await register(page, bob);

    // Bob's list is empty.
    await expect(page.getByText('No tasks yet')).toBeVisible();

    // And the API refuses a direct reference with 404, not 403, so it does not confirm the row.
    const statuses = await page.evaluate(async (id) => {
      const token = window.localStorage.getItem('todolist.accessToken');
      const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
      const base = `http://localhost:8080/api/v1/tasks/${id}`;
      return {
        read: (await fetch(base, { headers })).status,
        patch: (
          await fetch(`${base}/status`, {
            method: 'PATCH',
            headers,
            body: JSON.stringify({ status: 'COMPLETED' }),
          })
        ).status,
        remove: (await fetch(base, { method: 'DELETE', headers })).status,
      };
    }, taskId);

    expect(statuses).toEqual({ read: 404, patch: 404, remove: 404 });
  });
});
