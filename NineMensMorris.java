import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class NineMensMorris extends JFrame {

    // Board point class
    class Point {
        int x, y;
        int occupiedBy; // 0=empty, 1=human, 2=AI
        public Point(int x, int y){this.x=x; this.y=y; occupiedBy=0;}
        public boolean isEmpty(){return occupiedBy==0;}
    }

    // --- Game State Variables ---
    Point[] points = new Point[24];
    // All 16 possible mills
    int[][] mills = {
        {0,1,2},{3,4,5},{6,7,8},{9,10,11},
        {12,13,14},{15,16,17},{18,19,20},{21,22,23},
        {0,9,21},{3,10,18},{6,11,15},{1,4,7},
        {16,19,22},{8,12,17},{5,13,20},{2,14,23}
    };
    
    // Adjacency list for movement
    int[][] adjacency = {
        {0,1,9},{1,0,2,4},{2,1,14},{3,4,10},{4,1,3,5,7},{5,4,13},
        {6,7,11},{7,4,6,8},{8,7,12},{9,0,10,21},{10,3,9,11,18},{11,6,10,15},
        {12,8,13,17},{13,5,12,14,20},{14,2,13,23},{15,11,16},{16,15,17,19},{17,12,16},
        {18,10,19},{19,16,18,20,22},{20,13,19},{21,9,22},{22,19,21,23},{23,14,22}
    };

    boolean humanTurn=true;
    int humanPiecesPlaced=0, aiPiecesPlaced=0;
    int humanPiecesOnBoard=0, aiPiecesOnBoard=0;
    
    // States for piece removal after a mill
    boolean humanNeedsToRemovePiece = false;
    boolean aiNeedsToRemovePiece = false;
    
    Point selectedPoint=null;
    AIPlayer ai = new AIPlayer(this);

    // --- Constructor and Initialization ---
    public NineMensMorris(){
        setTitle("Nine Men's Morris");
        setSize(700,700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Points coordinates (same as original)
        int[][] coords={
            {50,50},{350,50},{650,50},{150,150},{350,150},{550,150},
            {250,250},{350,250},{450,250},{50,350},{150,350},{250,350},
            {450,350},{550,350},{650,350},{250,450},{350,450},{450,450},
            {150,550},{350,550},{550,550},{50,650},{350,650},{650,650}
        };
        for(int i=0;i<24;i++) points[i]=new Point(coords[i][0],coords[i][1]);

        // Mouse Listener for Human Moves
        addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                if(!humanTurn) return;

                for(Point p: points){
                    if(Math.abs(e.getX()-p.x)<20 && Math.abs(e.getY()-p.y)<20){
                        handleHumanClick(p);
                        break;
                    }
                }
                repaint();
            }
        });
        
        setVisible(true);
    }

    // --- Game Logic Methods ---
    
    void handleHumanClick(Point p) {
        if (humanNeedsToRemovePiece) {
            // Phase: Removing a piece
            if (p.occupiedBy == 2 && !isMill(2, p)) {
                p.occupiedBy = 0;
                aiPiecesOnBoard--;
                humanNeedsToRemovePiece = false;
                humanTurn = false;
                checkWin();
                SwingUtilities.invokeLater(() -> aiMove());
            } else if (p.occupiedBy == 2 && allPiecesAreInMills(2)) {
                 // Rule: Can remove a piece in a mill if ALL opponent pieces are in mills
                p.occupiedBy = 0;
                aiPiecesOnBoard--;
                humanNeedsToRemovePiece = false;
                humanTurn = false;
                checkWin();
                SwingUtilities.invokeLater(() -> aiMove());
            } else {
                JOptionPane.showMessageDialog(this, "You must remove an AI piece that is NOT in a mill, unless all AI pieces are in mills.");
            }
            return;
        }

        if (humanPiecesPlaced < 9) { 
            // Phase 1: Placing pieces
            if (p.isEmpty()){
                p.occupiedBy = 1;
                humanPiecesPlaced++;
                humanPiecesOnBoard++;
                if(checkMill(1, p)){
                    humanNeedsToRemovePiece = true;
                } else {
                    humanTurn = false;
                    SwingUtilities.invokeLater(() -> aiMove());
                }
            }
        } else { 
            // Phase 2/3: Moving or Flying
            if (selectedPoint == null && p.occupiedBy == 1){
                selectedPoint = p; // Select piece to move
            } else if (selectedPoint != null) {
                if (p == selectedPoint) {
                    selectedPoint = null; // Deselect
                } else if (p.isEmpty() && canMove(selectedPoint, p)){
                    p.occupiedBy = 1;
                    selectedPoint.occupiedBy = 0;
                    selectedPoint = null;
                    if(checkMill(1, p)){
                        humanNeedsToRemovePiece = true;
                    } else {
                        humanTurn = false;
                        checkWin();
                        SwingUtilities.invokeLater(() -> aiMove());
                    }
                } else if (p.occupiedBy == 1) {
                    selectedPoint = p; // Select a different piece
                }
            }
        }
    }

    // Check if the move is valid (adjacency or flying)
    boolean canMove(Point from, Point to){
        if(humanPiecesOnBoard == 3) return true; // Flying
        
        // Check adjacency
        int fromIdx = getPointIndex(from);
        int toIdx = getPointIndex(to);
        if (fromIdx == -1 || toIdx == -1) return false;

        for(int adj: adjacency[fromIdx]){
            if(adj == toIdx) return true;
        }
        return false;
    }

    // Helper to get index of a point
    int getPointIndex(Point p){
        for(int i=0;i<24;i++) if(points[i]==p) return i;
        return -1;
    }
    
    // Check if three pieces of 'player' form a mill after 'pt' was placed/moved.
    boolean checkMill(int player, Point pt){
        int idx = getPointIndex(pt);
        for(int[] mill: mills){
            if(Arrays.stream(mill).anyMatch(i->i==idx)){
                if(points[mill[0]].occupiedBy == player &&
                   points[mill[1]].occupiedBy == player &&
                   points[mill[2]].occupiedBy == player) return true;
            }
        }
        return false;
    }
    
    // Check if a specific point is part of an active mill
    boolean isMill(int player, Point p) {
        int idx = getPointIndex(p);
        for(int[] mill: mills){
            if(Arrays.stream(mill).anyMatch(i->i==idx)){
                if(points[mill[0]].occupiedBy == player &&
                   points[mill[1]].occupiedBy == player &&
                   points[mill[2]].occupiedBy == player) return true;
            }
        }
        return false;
    }
    
    // Check if ALL of 'player's pieces are in mills
    boolean allPiecesAreInMills(int player) {
        int piecesOnBoard = (player == 1) ? humanPiecesOnBoard : aiPiecesOnBoard;
        int millCount = 0;
        for (Point p : points) {
            if (p.occupiedBy == player) {
                if (isMill(player, p)) {
                    millCount++;
                }
            }
        }
        return millCount == piecesOnBoard;
    }

    // Main AI move execution (now handles all phases and removal)
    void aiMove(){
        if(aiNeedsToRemovePiece){
            // AI finds the best piece to remove
            int removeIdx = ai.findBestRemove(points);
            if(removeIdx != -1){
                points[removeIdx].occupiedBy = 0;
                humanPiecesOnBoard--;
            }
            aiNeedsToRemovePiece = false;
            humanTurn = true;
            checkWin();
            repaint();
            return;
        }
        
        // Find best placement or move/fly
        AIMove result = ai.findBestMove(points);

        if(result.type == AIMove.MoveType.PLACE){
            points[result.toIndex].occupiedBy = 2;
            aiPiecesPlaced++;
            aiPiecesOnBoard++;
            if(checkMill(2, points[result.toIndex])){
                aiNeedsToRemovePiece = true;
            }
        } else if (result.type == AIMove.MoveType.MOVE || result.type == AIMove.MoveType.FLY){
            points[result.fromIndex].occupiedBy = 0;
            points[result.toIndex].occupiedBy = 2;
            if(checkMill(2, points[result.toIndex])){
                aiNeedsToRemovePiece = true;
            }
        }

        humanTurn = true;
        checkWin();
        repaint();
        if(aiNeedsToRemovePiece) {
            SwingUtilities.invokeLater(() -> aiMove()); // Trigger removal immediately
        }
    }

    void checkWin(){
        if (humanPiecesPlaced == 9) {
             // Check if human has less than 3 pieces OR no valid moves left
            if(humanPiecesOnBoard < 3){
                JOptionPane.showMessageDialog(this,"AI Wins! (Human has < 3 pieces)");
                System.exit(0);
            }
            if(humanPiecesOnBoard >= 4 && !ai.canPlayerMove(1, points)){
                 JOptionPane.showMessageDialog(this,"AI Wins! (Human is blocked)");
                 System.exit(0);
            }
        }
        
        if (aiPiecesPlaced == 9) {
            // Check if AI has less than 3 pieces OR no valid moves left
            if(aiPiecesOnBoard < 3){
                JOptionPane.showMessageDialog(this,"Human Wins! (AI has < 3 pieces)");
                System.exit(0);
            }
            if(aiPiecesOnBoard >= 4 && !ai.canPlayerMove(2, points)){
                JOptionPane.showMessageDialog(this,"Human Wins! (AI is blocked)");
                System.exit(0);
            }
        }
    }

    // --- Drawing/GUI (Visual Enhancements) ---
    @Override
    public void paint(Graphics g){
        super.paint(g);
        Graphics2D g2=(Graphics2D) g;
        
        // Background and Board lines
        g2.setColor(new Color(139, 69, 19)); // Deep Brown (Wood)
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        g2.setStroke(new BasicStroke(3));
        g2.setColor(Color.BLACK);

        // Board rectangles
        g2.drawRect(50,50,600,600);
        g2.drawRect(150,150,400,400);
        g2.drawRect(250,250,200,200);

        // Connecting lines
        g2.drawLine(350,50,350,250);
        g2.drawLine(50,350,250,350);
        g2.drawLine(450,350,650,350);
        g2.drawLine(350,450,350,650);

        // Draw points and pieces
        for(Point p: points){
            Color pieceColor;
            if(p.occupiedBy==0) {
                pieceColor = Color.LIGHT_GRAY; // Empty point
                g2.setColor(pieceColor);
                g2.fillOval(p.x-10,p.y-10,20,20);
            } else {
                if(p.occupiedBy==1) pieceColor = new Color(192, 192, 192); // Silver (Human)
                else pieceColor = new Color(184, 134, 11); // Brass/Gold (AI)
                
                g2.setColor(pieceColor);
                g2.fillOval(p.x-15,p.y-15,30,30);
                
                // Highlight selected piece
                if(p==selectedPoint) {
                    g2.setColor(Color.GREEN);
                    g2.drawOval(p.x-18,p.y-18,36,36);
                }
            }
            g2.setColor(Color.BLACK);
            g2.drawOval(p.x-15,p.y-15,30,30);
        }
        
        // Display Game Status
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        String status = "Turn: " + (humanTurn ? "Human (Silver)" : "AI (Brass)");
        if (humanNeedsToRemovePiece) status = "HUMAN: Remove an AI piece!";
        if (aiNeedsToRemovePiece) status = "AI is removing a piece...";
        
        g2.drawString(status, 10, 40);
        g2.drawString("Human: " + humanPiecesOnBoard + " on board (" + (9 - humanPiecesPlaced) + " left to place)", 500, 20);
        g2.drawString("AI: " + aiPiecesOnBoard + " on board (" + (9 - aiPiecesPlaced) + " left to place)", 500, 40);
    }

    // --- Main Method ---
    public static void main(String[] args){
        new NineMensMorris();
    }
    
    // --- AI Helper Class ---
    class AIMove {
        enum MoveType {PLACE, MOVE, FLY, NONE}
        MoveType type = MoveType.NONE;
        int fromIndex = -1;
        int toIndex = -1;
        public AIMove(MoveType type, int to){ this.type=type; this.toIndex=to; } // For PLACING
        public AIMove(MoveType type, int from, int to){ this.type=type; this.fromIndex=from; this.toIndex=to; } // For MOVING/FLYING
    }

    class AIPlayer{
        int maxDepth=3;
        NineMensMorris game;
        
        public AIPlayer(NineMensMorris game) { this.game = game; }

        // Main function to find the best move (Place/Move/Fly)
        public AIMove findBestMove(Point[] boardPoints){
            if (game.aiPiecesPlaced < 9) {
                return findBestPlaceMove(boardPoints); // Placing Phase
            } else {
                return findBestMoveFlyMove(boardPoints); // Moving/Flying Phase
            }
        }
        
        // Finds the best empty spot to place a piece
        private AIMove findBestPlaceMove(Point[] boardPoints) {
            int bestVal = Integer.MIN_VALUE;
            int bestIndex = -1;
            
            for(int i=0; i<24; i++){
                if(boardPoints[i].occupiedBy == 0){
                    
                    // 1. Simulate placement
                    boardPoints[i].occupiedBy = 2; 
                    
                    // 2. Check for an immediate mill
                    int eval = 0;
                    if(game.checkMill(2, boardPoints[i])) {
                        // Priority: If a mill is formed, the AI should get a huge bonus
                        eval += 1000;
                        // Since we can't search the removal in the main minimax,
                        // we just take the huge bonus for forming the mill.
                    }
                    
                    // 3. Add Minimax evaluation
                    eval += alphaBeta(boardPoints, maxDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                    
                    // 4. Backtrack
                    boardPoints[i].occupiedBy = 0;
                    
                    if(eval > bestVal){
                        bestVal = eval;
                        bestIndex = i;
                    }
                }
            }
            if (bestIndex != -1) return new AIMove(AIMove.MoveType.PLACE, bestIndex);
            return new AIMove(AIMove.MoveType.NONE, -1);
        }
        
        // Finds the best move (from/to) for the Moving/Flying phase
        private AIMove findBestMoveFlyMove(Point[] boardPoints) {
            int bestVal = Integer.MIN_VALUE;
            AIMove bestMove = new AIMove(AIMove.MoveType.NONE, -1);
            boolean isFlying = game.aiPiecesOnBoard == 3;
            
            for(int i=0; i<24; i++){ // iterate through 'from' points
                if(boardPoints[i].occupiedBy == 2){
                    
                    for(int j=0; j<24; j++){ // iterate through 'to' points
                        if(boardPoints[j].occupiedBy == 0){
                            
                            if (isFlying || game.canMove(boardPoints[i], boardPoints[j])) {
                                
                                AIMove.MoveType type = isFlying ? AIMove.MoveType.FLY : AIMove.MoveType.MOVE;
                                
                                // 1. Simulate move
                                boardPoints[i].occupiedBy = 0;
                                boardPoints[j].occupiedBy = 2;
                                
                                int eval = 0;
                                
                                // 2. Check for an immediate mill
                                if(game.checkMill(2, boardPoints[j])) {
                                    eval += 1000;
                                }
                                
                                // 3. Add Minimax evaluation
                                eval += alphaBeta(boardPoints, maxDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                                
                                // 4. Backtrack
                                boardPoints[j].occupiedBy = 0;
                                boardPoints[i].occupiedBy = 2;
                                
                                if(eval > bestVal){
                                    bestVal = eval;
                                    bestMove = new AIMove(type, i, j);
                                }
                            }
                        }
                    }
                }
            }
            return bestMove;
        }

        // Minimax with Alpha-Beta Pruning (handles placing only in this simplified version)
        int alphaBeta(Point[] boardPoints, int depth, int alpha, int beta, boolean maximizing){
            // Win/Loss check (simple version)
            if(game.humanPiecesOnBoard < 3 && game.humanPiecesPlaced == 9) return 10000;
            if(game.aiPiecesOnBoard < 3 && game.aiPiecesPlaced == 9) return -10000;
            
            if(depth == 0) return evaluate(boardPoints);

            // This basic minimax only considers the placing phase for simplicity in the search tree.
            // A perfect AI would search for all possible moves (place, move, fly, remove) at each step.
            
            if(maximizing){ // AI's turn (Player 2)
                int maxEval = Integer.MIN_VALUE;
                for(int i=0; i<24; i++){
                    if(boardPoints[i].occupiedBy == 0){ // Check empty spots for placing a piece
                        boardPoints[i].occupiedBy = 2;
                        int eval = alphaBeta(boardPoints, depth-1, alpha, beta, false);
                        boardPoints[i].occupiedBy = 0;
                        maxEval = Math.max(maxEval, eval);
                        alpha = Math.max(alpha, eval);
                        if(beta <= alpha) break;
                    }
                }
                return maxEval;
            } else { // Human's turn (Player 1)
                int minEval = Integer.MAX_VALUE;
                for(int i=0; i<24; i++){
                    if(boardPoints[i].occupiedBy == 0){ // Check empty spots for placing a piece
                        boardPoints[i].occupiedBy = 1;
                        int eval = alphaBeta(boardPoints, depth-1, alpha, beta, true);
                        boardPoints[i].occupiedBy = 0;
                        minEval = Math.min(minEval, eval);
                        beta = Math.min(beta, eval);
                        if(beta <= alpha) break;
                    }
                }
                return minEval;
            }
        }
        
        // Finds the best human piece to remove after forming a mill
        public int findBestRemove(Point[] boardPoints){
            int bestVal = Integer.MIN_VALUE;
            int bestIndex = -1;

            for(int i=0; i<24; i++){
                Point p = boardPoints[i];
                // Must be a human piece (1) AND not in a mill (unless all pieces are in mills)
                if(p.occupiedBy == 1 && (!game.isMill(1, p) || game.allPiecesAreInMills(1))){
                    
                    // 1. Simulate removal
                    p.occupiedBy = 0;
                    
                    // 2. Evaluate the resulting board state (depth 1 search after removal)
                    int val = alphaBeta(boardPoints, 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                    
                    // 3. Backtrack
                    p.occupiedBy = 1;
                    
                    if(val > bestVal){
                        bestVal = val;
                        bestIndex = i;
                    }
                }
            }
            return bestIndex;
        }

        // More comprehensive evaluation function (Heuristic)
        int evaluate(Point[] boardPoints){
            int score = 0;
            int aiPieces = 0, humanPieces = 0;
            
            for(Point p: boardPoints){
                if(p.occupiedBy == 2) aiPieces++;
                if(p.occupiedBy == 1) humanPieces++;
            }
            
            // 1. Difference in pieces
            score += (aiPieces - humanPieces) * 10;
            
            // 2. Open Mills (2-in-a-row with an empty spot)
            int aiOpenMills = 0;
            int humanOpenMills = 0;
            
            for(int[] mill : game.mills) {
                int countAI = 0;
                int countHuman = 0;
                int emptySpot = -1;
                
                for(int index : mill) {
                    if (boardPoints[index].occupiedBy == 2) countAI++;
                    else if (boardPoints[index].occupiedBy == 1) countHuman++;
                    else emptySpot = index;
                }
                
                if (countAI == 2 && countHuman == 0 && emptySpot != -1) {
                    aiOpenMills++; // AI has a 2-piece threat
                }
                if (countHuman == 2 && countAI == 0 && emptySpot != -1) {
                    humanOpenMills++; // Human has a 2-piece threat
                }
            }
            
            score += aiOpenMills * 50; // High value for creating threats
            score -= humanOpenMills * 50; // High penalty for allowing threats
            
            return score;
        }
        
        // Checks if a player has any valid move (used for win condition)
        public boolean canPlayerMove(int player, Point[] boardPoints) {
            boolean isFlying = (player == 1 ? game.humanPiecesOnBoard : game.aiPiecesOnBoard) == 3;
            
            for (int i = 0; i < 24; i++) {
                if (boardPoints[i].occupiedBy == player) { // Piece to move
                    for (int j = 0; j < 24; j++) {
                        if (boardPoints[j].occupiedBy == 0) { // Empty destination
                            if (isFlying || game.canMove(boardPoints[i], boardPoints[j])) {
                                return true; // Found at least one valid move
                            }
                        }
                    }
                }
            }
            return false;
        }
    }
}