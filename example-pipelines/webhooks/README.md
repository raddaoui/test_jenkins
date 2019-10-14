# example-pipelines/webhooks 
Example `.json` files describing GitLab webhooks created by `Automation/create-gitlab-webhooks` in Jenkins

## Usage
- Place `.json` files in your GitLab Project's `infra/jenkins/webhooks/` directory.
- The target URL of a webhook is treated as a unique identifier across all webhooks in the project they are created in - if you have more than one `.json` file with the same `id` and `url` attribute values, they will overwrite each other.
- Follow the [GitLab API documentation on Project Hooks](https://docs.gitlab.com/ce/api/projects.html#hooks) for more details on attributes.
- In order to find a GitLab Project ID for the `id` attribute of your `.json` file, visit `Settings > General > General project settings` and look for the `Project ID` field.  If you don't have permissions in GitLab to see this section, you can use [Namespaced Path Encoding](https://docs.gitlab.com/ee/api/README.html#namespaced-path-encoding) instead of an ID.

