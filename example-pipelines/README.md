# gcp-jenkins/example-pipelines
Example pipelines can be found here. Please refer to the following documents for additional information:
- [Getting Started With Pipelines](https://jenkins.io/doc/book/pipeline/getting-started/)
- [Pipeline Syntax](https://jenkins.io/doc/book/pipeline/syntax/)
- [Top 10 Best Practices for Jenkins Pipelines](https://www.cloudbees.com/blog/top-10-best-practices-jenkins-pipeline-plugin)

## Use Cases
- [template](template.groovy): Use this as a template if you need to roll your own pipeline. Use one of the below examples if the use case meets your needs.
- [sample-pipeline](sample-deploy.groovy): Start here first and see if it meets your needs. This one is much cleaner with regards to automatic triggering.
- [parameterized-pipeline](parameterized-deploy.groovy): If you have a more advanced workflow which requires parameters, try this one. It requires a 'refresh' parameter to update the XML beneath the job definition based on what is in the Groovy code. This is not as clean with automation.
- [pipeline-router](default-pipeline-router.groovy): Triggered by push-event webhooks from GitLab, this pipeline calls a downstream environment-specific deployment jobs based on branch metadata from the webhook payload.  This is recommended for continuous delivery on pushed merge requests.
- [rolling-redeploy](rolling-redeploy.groovy): If you are using something like a uMIG and need to upgrade your cluster without taking all nodes down at once.
- [destroy-all-apply-single](destroy-all-apply-single.groovy): If you are upgrading something like a backend with a version-incompatible change and need to take everything down at once and redeploy one at a time for manual verification.
- [auto_discover_pipelines_config](auto_discover_pipelines_config.json): Optionally modify the behavior of Jenkins' `auto-discover-pipelines` job.
- [timeouts](timeouts.groovy): Apply timeouts to various scopes of a pipeline.

## The standard library
There is now a library of standard scripts that can be used to apply, taint, and destroy resources as well as be used by merge request validation pipelines to ensure that Terraform code is working and deployable.

Both the [simple pipeline](sample-deploy.groovy) and [parameterized pipeline](parameterized-deploy.groovy) use the standard library so check them out for examples of how to start using the library.

See its [repository](https://gitlab.marketo.org/GCP-Infrastructure/gcp-jenkins-stdlib) for more information.

## auto_discover_pipelines_config.json

This file configures the behavior of the `auto-discover-pipelines` job.  This
file allows a repository to have branches discovered and imported which would
otherwise be skipped.  For example, an unprotected branch may need to be
discovered and deployed, or a protected branch which is otherwise skipped (e.g.
master.)

__NOTE__ The configuration file must exist in the default branch of the
repository to be used.  The configuration file must be exactly named
`infra/jenkins/auto_discover_pipelines_config.json`.

Protected branches are imported unless `"skip_protected_branches": true`.

`"skip_protected_branches": true | false`

 * If `true`, `auto-discover-pipelines` will not look for protected branches
   to import, it will _only_ use the provided list.
 * If `false`, `auto-discover-pipelines` will use the provided list of
   branches in _addition_ to any which are found to be protected and contain
   valid `.groovy` files.

`"import_branches": [ "foo", "bar" ]`

 * `auto-discover-pipelines` will import `.groovy` files from these branches
   whether the branch is protected or not.

### Examples


Import the master branch, which may or may not be protected, in addition to all
protected branches:

```json
{
  "import_branches": [
    "master"
  ]
}
```

Import only the master branch which may or may not be protected:

```json
{
  "skip_protected_branches": true,
  "import_branches": [
    "master"
  ]
}
```

Import the dev branch which may be unprotected, in addition to all other
protected branches:

```json
{
  "import_branches": [
    "dev"
  ]
}
```
