$(document).ready(function() {
    let currentQuestion = null;
    let selectedOption = null;

    // Generate question button click handler
    $('#generateBtn').click(function() {
        const subject = $('#subject').val();
        const topic = $('#topic').val();
        const difficulty = $('#difficulty').val();

        if (!subject) {
            alert('Please select a subject');
            return;
        }

        // Show loading state
        const $btn = $(this);
        $btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Generating...');

        // Call the backend to generate a question
        $.ajax({
            url: '/generate',
            method: 'POST',
            data: {
                subject: subject,
                topic: topic || 'General',
                difficulty: difficulty
            },
            success: function(response) {
                try {
                    currentQuestion = typeof response === 'string' ? JSON.parse(response) : response;
                    displayQuestion(currentQuestion);
                    $('#questionContainer').removeClass('d-none');
                    $('#fallbackIndicator').remove(); // Remove fallback indicator if present
                } catch (e) {
                    console.error('Error parsing question:', e);
                    alert('Error generating question. Please try again.');
                }
            },
            error: function(xhr, status, error) {
                console.error('Error:', error);
                alert('Error generating question. Please try again.');
            },
            complete: function() {
                $btn.prop('disabled', false).text('Generate Question');
            }
        });
    });

    // Display question and options
    function displayQuestion(question) {
        $('#questionText').text(question.questionText);
        const $optionsList = $('#optionsList').empty();

        question.options.forEach((option, index) => {
            const $option = $(`
                <button type="button" class="list-group-item list-group-item-action option"
                        data-index="${index}">
                    ${String.fromCharCode(65 + index)}. ${option}
                </button>
            `);
            $optionsList.append($option);
        });

        // Show fallback indicator if this is a fallback question
        if (question.isFallback) {
            const $indicator = $(`
                <div id="fallbackIndicator" class="alert alert-warning mt-2" role="alert">
                    <small><i class="fas fa-exclamation-triangle"></i> This is a sample question. API integration may not be working properly.</small>
                </div>
            `);
            $('#questionContainer').prepend($indicator);
        }

        // Reset UI
        $('#feedback').empty().removeClass('alert-success alert-danger');
        $('#checkAnswerBtn').prop('disabled', false).text('Check Answer');
        $('.option').prop('disabled', false);
        selectedOption = null;
    }

    // Option selection
    $(document).on('click', '.option', function() {
        $('.option').removeClass('active');
        $(this).addClass('active');
        selectedOption = $(this).data('index');
    });

    // Check answer
    $('#checkAnswerBtn').click(function() {
        if (selectedOption === null) {
            alert('Please select an option');
            return;
        }

        const isCorrect = selectedOption === currentQuestion.correctAnswerIndex;
        const feedback = isCorrect ? 'Correct! 🎉' : `Incorrect. The correct answer is: ${String.fromCharCode(65 + currentQuestion.correctAnswerIndex)}. ${currentQuestion.options[currentQuestion.correctAnswerIndex]}`;
        
        $('#feedback')
            .text(feedback)
            .addClass(isCorrect ? 'alert alert-success' : 'alert alert-danger');
        
        // Disable options after checking
        $('.option').prop('disabled', true);
        $(this).prop('disabled', true);
    });

    // Next question
    $('#nextQuestionBtn').click(function() {
        $('#generateBtn').click();
    });
});
