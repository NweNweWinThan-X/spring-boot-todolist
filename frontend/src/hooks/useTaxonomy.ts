import { useCallback, useEffect, useState } from 'react';
import { toast } from 'sonner';
import * as labelApi from '../api/labels';
import * as projectApi from '../api/projects';
import { toErrorMessage } from '../api/client';
import type { Label, Project } from '../types';

/** Projects and labels, which the sidebar and the task modal both need. */
export function useTaxonomy() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [labels, setLabels] = useState<Label[]>([]);

  const load = useCallback(async () => {
    try {
      const [p, l] = await Promise.all([projectApi.fetchProjects(), labelApi.fetchLabels()]);
      setProjects(p);
      setLabels(l);
    } catch (error) {
      toast.error(toErrorMessage(error, 'Could not load projects and labels.'));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const addProject = useCallback(
    async (name: string, color: string) => {
      try {
        await projectApi.createProject({ name, color });
        toast.success('Project added');
        await load();
      } catch (error) {
        toast.error(toErrorMessage(error, 'Could not add the project.'));
      }
    },
    [load],
  );

  const addLabel = useCallback(
    async (name: string, color: string) => {
      try {
        await labelApi.createLabel({ name, color });
        toast.success('Label added');
        await load();
      } catch (error) {
        toast.error(toErrorMessage(error, 'Could not add the label.'));
      }
    },
    [load],
  );

  return { projects, labels, addProject, addLabel, reload: load };
}
