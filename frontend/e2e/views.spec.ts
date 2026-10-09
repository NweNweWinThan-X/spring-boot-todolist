import { expect, test } from '@playwright/test';
import { register, uniqueAccount } from './helpers';

/** Creates a task through the API, so view fixtures do not depend on the UI. */
async function seed(page: import('@playwright/test').Page, body: Record<string, unknown>) {
  const status = await page.evaluate(async (payload) => {
    const token = window.localStorage.getItem('todolist.accessToken');
    const res = await fetch('http://localhost:8080/api/v1/tasks', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(payload),
    });
    return res.status;
  }, body);
  expect(status).toBe(201);
}

/**
 * Local calendar date, not `toISOString()`. The server resolves "today" with a system-default
 * -zone Clock, so a UTC date is off by one whenever the machine is ahead of UTC.
 */
function isoDate(offsetDays: number): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

test.describe('Views and the Eisenhower matrix', () => {
  test.beforeEach(async ({ page }) => {
    await register(page, uniqueAccount());
    await seed(page, {
      title: 'Due today',
      status: 'PENDING',
      priority: 'HIGH',
      dueDate: isoDate(0),
      urgent: true,
      important: true,
    });
    await seed(page, {
      title: 'Overdue item',
      status: 'PENDING',
      priority: 'MEDIUM',
      dueDate: isoDate(-3),
    });
    await seed(page, {
      title: 'Later this week',
      status: 'PENDING',
      priority: 'LOW',
      dueDate: isoDate(3),
      important: true,
    });
    await seed(page, { title: 'Unfiled note', status: 'PENDING', priority: 'LOW' });
    await page.reload();
  });

  test('Today shows only what is due today', async ({ page }) => {
    await page.getByRole('button', { name: 'Today' }).click();
    await expect(page.getByTestId('task-card')).toHaveCount(1);
    await expect(page.getByTestId('task-card')).toContainText('Due today');
  });

  test('Overdue shows past, unfinished tasks and marks them', async ({ page }) => {
    await page.getByRole('button', { name: 'Overdue' }).click();
    await expect(page.getByTestId('task-card')).toHaveCount(1);
    await expect(page.getByTestId('task-card')).toContainText('Overdue item');
    await expect(page.getByTestId('task-card')).toContainText('overdue');
  });

  test('Upcoming covers the next seven days', async ({ page }) => {
    await page.getByRole('button', { name: 'Upcoming' }).click();
    await expect(page.getByTestId('task-card')).toHaveCount(2);
  });

  test('Inbox shows tasks with no project', async ({ page }) => {
    await page.getByRole('button', { name: 'Inbox' }).click();
    await expect(page.getByTestId('task-card')).toHaveCount(4);
  });

  test('the matrix places tasks by urgency and importance', async ({ page }) => {
    await page.getByRole('button', { name: 'Eisenhower matrix' }).click();

    await expect(page.getByTestId('quadrant-DO')).toContainText('Due today');
    await expect(page.getByTestId('quadrant-SCHEDULE')).toContainText('Later this week');
    await expect(page.getByTestId('quadrant-ELIMINATE')).toContainText('Unfiled note');
    await expect(page.getByTestId('quadrant-DELEGATE')).toContainText('Empty');
  });

  test('progress reflects completion for today', async ({ page }) => {
    const card = page.getByTestId('task-card').filter({ hasText: 'Due today' });
    await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '0');
    await card.getByRole('checkbox').click();
    await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
  });
});
