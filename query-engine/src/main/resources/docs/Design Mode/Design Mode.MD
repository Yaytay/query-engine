# Design Mode

> :warning: Warning: 
> DO NOT USE DESIGN MODE FOR PRODUCTION
>
> Containers running with the design-mode image are insecure and must not be accessible to external users as 
> it allows any user with access to the API to read/write your queries.

Design Mode extends the API and UI of Query Engine to provide a supported environment for designing queries.

The expected setup for this is:
* Keep all your query files in a git repository.
  You can split your files across multiple repositories if that suits your internal model better.
  Designate one branch of this repo as the live branch (I'm going to assume "main", but you can make it anything).
* Clone the repo to somewhere accessible by your production system and mount it in the production container.
* Use a CI pipeline to keep that instance up to date with any changes to that branch.
  The precise mechanism for doing this is out of scope for this documentation, but an example will be provided when I get around to it.

Given the above, your query developers should then:
* Clone the query repository.
* Run Query Engine in Design Mode against their local clone of the repository.
* Make changes to the query files.
  These changes will only be made to the files in the local copy of the repository.
* When all changes have been made and tested the query developers should push their changes to the repository.
  At which point the CI pipeline will make them live.

> :information_source: Note: Design Mode will only permit the editing of Velocity templates as plain text files.
>
> There are much better text editors than Design Mode for editing Velocity templates.
> As a general rule you should stop using Design Mode when you start using Velocity templates.
