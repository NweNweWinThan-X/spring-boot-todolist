import { test } from '@playwright/test';
import { quickAdd, register, uniqueAccount } from './helpers';

/**
 * Captures the images used by docs/TESTING.md. Not an assertion suite — run it with
 * `npm run screenshots` after a UI change to refresh the documentation.
 */
const DIR = '../docs/screenshots';

function isoDate(offsetDays: number): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

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
  // Ids are global sequences, so a hard-coded projectId would belong to another account and
  // silently 404. Fail loudly instead of producing a half-empty screenshot.
  if (status !== 201) {
    throw new Error(`seed failed with ${status}: ${JSON.stringify(body)}`);
  }
}

async function seedTaxonomy(page: import('@playwright/test').Page) {
  return page.evaluate(async () => {
    const token = window.localStorage.getItem('todolist.accessToken');
    const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
    const projectIds: number[] = [];
    for (const p of [
      { name: 'Work', color: '#8B5CF6' },
      { name: 'Personal', color: '#EC4899' },
    ]) {
      const res = await fetch('http://localhost:8080/api/v1/projects', {
        method: 'POST',
        headers,
        body: JSON.stringify(p),
      });
      projectIds.push((await res.json()).id);
    }
    const labelIds: number[] = [];
    for (const l of [
      { name: 'urgent', color: '#F43F5E' },
      { name: 'deep-work', color: '#22D3EE' },
    ]) {
      const res = await fetch('http://localhost:8080/api/v1/labels', {
        method: 'POST',
        headers,
        body: JSON.stringify(l),
      });
      labelIds.push((await res.json()).id);
    }
    return { projectIds, labelIds };
  });
}

test.use({ viewport: { width: 1280, height: 900 } });

test('capture documentation screenshots', async ({ page }) => {
  // 01 — login
  await page.goto('/login');
  await page.screenshot({ path: `${DIR}/01-login.png` });

  // 02 — register
  await page.goto('/register');
  await page.screenshot({ path: `${DIR}/02-register.png` });

  // 03 — empty dashboard
  const account = uniqueAccount();
  await register(page, account);
  await page.screenshot({ path: `${DIR}/03-dashboard-empty.png` });

  // Seed a realistic board
  const { projectIds, labelIds } = await seedTaxonomy(page);
  await seed(page, {
    title: 'Design review with the platform team',
    description: 'Walk through the new module boundaries',
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    dueDate: isoDate(0),
    startTime: '18:00',
    endTime: '20:00',
    alertEnabled: true,
    urgent: true,
    important: true,
    projectId: projectIds[0],
    labelIds: [labelIds[0]],
  });
  await seed(page, {
    title: 'Write the quarterly plan',
    status: 'PENDING',
    priority: 'MEDIUM',
    dueDate: isoDate(4),
    important: true,
    projectId: projectIds[0],
    labelIds: [labelIds[1]],
  });
  await seed(page, {
    title: 'Renew the domain name',
    status: 'PENDING',
    priority: 'HIGH',
    dueDate: isoDate(-2),
    urgent: true,
  });
  await seed(page, {
    title: 'Buy milk and eggs',
    status: 'COMPLETED',
    priority: 'LOW',
    dueDate: isoDate(0),
  });
  await page.reload();
  await page.getByTestId('task-card').first().waitFor();

  // 04 — populated dashboard (dark, the default)
  await page.screenshot({ path: `${DIR}/04-dashboard-dark.png` });

  // 05 — light theme
  await page.getByRole('button', { name: 'Switch to light mode' }).click();
  await page.waitForTimeout(250);
  await page.screenshot({ path: `${DIR}/05-dashboard-light.png` });
  await page.getByRole('button', { name: 'Switch to dark mode' }).click();
  await page.waitForTimeout(250);

  // 06 — Today view
  await page.getByRole('button', { name: 'Today' }).click();
  await page.getByTestId('task-card').first().waitFor();
  await page.screenshot({ path: `${DIR}/06-view-today.png` });

  // 07 — Overdue view
  await page.getByRole('button', { name: 'Overdue' }).click();
  await page.getByTestId('task-card').first().waitFor();
  await page.screenshot({ path: `${DIR}/07-view-overdue.png` });

  // 08 — Eisenhower matrix
  await page.getByRole('button', { name: 'All tasks' }).click();
  await page.getByRole('button', { name: 'Eisenhower matrix' }).click();
  await page.getByTestId('quadrant-DO').waitFor();
  await page.screenshot({ path: `${DIR}/08-matrix.png` });

  // 09 — edit modal
  await page.getByRole('button', { name: 'Eisenhower matrix' }).click();
  await page.getByRole('button', { name: /^Edit Design review/ }).click();
  await page.getByTestId('task-modal').waitFor();
  await page.screenshot({ path: `${DIR}/09-task-modal.png` });
  await page.getByRole('button', { name: 'Close' }).click();

  // 10 — validation error toast
  await page.getByTestId('quick-add-input').fill('<script>alert(1)</script>');
  await page.getByTestId('quick-add-submit').click();
  await page.getByText('使用できない文字が含まれています').waitFor();
  await page.screenshot({ path: `${DIR}/10-validation-error.png` });

  // 11 — delete confirmation
  await page.reload();
  await page.getByTestId('task-card').first().waitFor();
  await page.getByRole('button', { name: /^Delete Renew the domain/ }).click();
  await page.getByTestId('confirm-dialog').waitFor();
  await page.screenshot({ path: `${DIR}/11-delete-confirm.png` });
  await page.getByRole('button', { name: 'Cancel' }).click();

  // 12 — search
  await page.getByTestId('search').fill('plan');
  await page.waitForTimeout(600);
  await page.screenshot({ path: `${DIR}/12-search.png` });

  // 13 — mobile
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByTestId('search').fill('');
  await page.waitForTimeout(600);
  await page.screenshot({ path: `${DIR}/13-mobile.png`, fullPage: false });
});
