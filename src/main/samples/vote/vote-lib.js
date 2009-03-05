
function updateBallots(ballots, newBallot, email) {
  b = <ballot>{newBallot.text()}</ballot>;
  b.@email = email;
  ballots.appendChild(b);
  return ballots;
}

function getCurrentTally(ballots) {
  tally = <tally></tally>;
  for each (var b in ballots.ballot) {
    if (tally.vote.(@text == b.text()).length() == 0) {
      tally.appendChild( <vote text={b.text()}>1</vote> );
    } else {
      v = tally.vote.(@text == b.text());
      v.setChildren(parseInt(v.text()) + 1)
    }
  }
  return tally;
}

function getUserBallot(ballots, email) {
  return ballots.ballot.(@email == email);
}

