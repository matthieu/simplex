
function updateBallots(ballots, newBallot, email) {
  b = <ballot>{ballot}</ballot>;
  b.@email = email;
  ballots.appendChild(b);
  return ballots;
}

function getCurrentTally(ballots) {
  tally = <tally></tally>;
  for each (var b in ballots.ballot) {
    if (tally.(@text == b).length() == 0) {
      tally.vote = <vote text="">1</vote>;
    } else {
      tally.vote(@text == b) += 1;
    }
  }
  return tally;
}

function getUserBallot(ballots, email) {
  return ballots.ballot.(@email == email);
}

